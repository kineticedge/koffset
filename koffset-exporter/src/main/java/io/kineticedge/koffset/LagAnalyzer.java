package io.kineticedge.koffset;

import io.kineticedge.koffset.config.CollectorConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// time since last successful run
// execution time
// metric size?
// expired groups
// failed groups
// rebalancing groups

public class LagAnalyzer {


    private static final Logger log = LoggerFactory.getLogger(LagAnalyzer.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

    private static final int VELOCITY_WINDOW_SIZE = 5; // Average over last 5 scrapes


    public record OffsetInfo(long offset, long timestamp) {
    }

    /*
    Latest Offset Broker Yes Producer is still sending data.
Group Offset Broker No Consumer is not processing.
Group Timestamp Interpolation No That specific offset was written at a fixed point in the past.
Observed Timestamp Local Clock No It marks the start of the stuck period.
Lag (Seconds) Math Yes The gap between the growing Head and the stationary Group is increasing.
     */
    public record LagDetail(
            long partitionHeadOffset,
            long partitionHeadTimestamp,
            long groupCommittedOffset,
            long groupOffsetInterpolatedTimestamp, // interpolated broker time
            long groupOffsetFirstObservedTimestamp, // wall clock time when we first polled this offset
            long offsetLag,
            double groupVelocityRecordsPerSec
    ) {
    }

    public record GroupSnapshot(Map<String, ConsumerGroupDescription> metadata, Map<String, Map<TopicPartition, Long>> offsets) {
    }

    //

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final CollectorConfig config;
    private final Admin admin;

    private final Map<String, Map<TopicPartition, LagDetail>> groupLag = new ConcurrentHashMap<>();

    //TODO move to ref so it can be atomically handled
    private final java.util.concurrent.atomic.AtomicReference<Map<String, Map<TopicPartition, LagDetail>>> groupLagRef =
            new java.util.concurrent.atomic.AtomicReference<>(Collections.emptyMap());


    private final Map<String, ConsumerGroupDescription> groupMetadata = new ConcurrentHashMap<>();
    private final Map<TopicPartition, Deque<long[]>> producerHistory = new ConcurrentHashMap<>();
    private final Map<String, Map<TopicPartition, Deque<long[]>>> offsetHistory = new ConcurrentHashMap<>();


    private volatile long lastRefreshDurationMs = 100; // Default estimate
    private long lastIntervalMs;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;
    private final long maxFutureDeltaMs = 3600_000L;
    private long refreshedAt;

    //private long timeoutMs = 30_000L;

    //private Server server;

    public LagAnalyzer(final CollectorConfig config, final Admin admin) {
        this.config = config;
        this.admin = admin;
    }

    private long timeoutMs() {
        return config.getAdminTimeout();
    }

    private final long minimalRefreshMs = 50L;

    public void start() {

        this.refreshedAt = System.currentTimeMillis();
        this.lastIntervalMs = config.getInitialInterval();

        // synchronous hydration, minimizes misinformation on restart
        try {
            log.info("preloading producer history");
            hydrateProducerHistory();
        } catch (Exception e) {
            log.error("failed to hydrate history on startup; data may be inaccurate until producer moves", e);
        }

        schedule(config.getInitialDelay(), config.getInitialInterval());
    }

    public Map<String, Map<TopicPartition, LagDetail>> lag() {
        return groupLag;
    }

    public Map<String, ConsumerGroupDescription> groupMetadata() {
        return groupMetadata;
    }

    public long getRefreshedAt() {
        return refreshedAt;
    }

    private void schedule(long initialDelayMs, long delayMs) {
        log.info("Scheduling refresh initialDelay={}, period={}", initialDelayMs, delayMs);
        scheduledTask = executor.scheduleAtFixedRate(this::refresh, initialDelayMs, delayMs, TimeUnit.MILLISECONDS);
    }

    public void reschedule(long targetIntervalMs, long delayMs) {

        // adjust last interval to the large interval
        this.lastIntervalMs = targetIntervalMs;

        log.info("Rescheduling refresh targetIntervalMs(lastIntervalMs)={}, initialDelay={}, period={}", targetIntervalMs, delayMs, lastIntervalMs);


        scheduledTask.cancel(false);
        scheduledTask = executor.scheduleAtFixedRate(this::refresh, delayMs, lastIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void hydrateProducerHistory() throws Exception {

        List<String> ids = admin.listGroups().all().get(timeoutMs(), TimeUnit.MILLISECONDS)
                .stream().map(GroupListing::groupId).toList();

        if (ids.isEmpty()) return;

        Map<String, ListConsumerGroupOffsetsSpec> specs = ids.stream()
                .collect(Collectors.toMap(id -> id, id -> new ListConsumerGroupOffsetsSpec()));

        Set<TopicPartition> partitions = admin.listConsumerGroupOffsets(specs).all().get(timeoutMs(), TimeUnit.MILLISECONDS)
                .values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet());

        if (partitions.isEmpty()) return;

        // 1. Get the extreme poles (Earliest and Latest)
        var earliest = admin.listOffsets(partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.forTimestamp(0L))))
                .all().get(timeoutMs(), TimeUnit.MILLISECONDS);
        var latest = admin.listOffsets(partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.maxTimestamp())))
                .all().get(timeoutMs(), TimeUnit.MILLISECONDS);

        // 2. Prepare percentile queries
        Map<TopicPartition, Deque<long[]>> results = new HashMap<>();

        partitions.forEach(tp -> {
            var start = earliest.get(tp);
            var end = latest.get(tp);
            if (start == null || end == null || start.timestamp() <= 0 || end.timestamp() <= start.timestamp()) return;

            var history = producerHistory.computeIfAbsent(tp, k -> new ArrayDeque<>());
            history.addLast(new long[]{start.offset(), start.timestamp()});

            // Calculate time-based midpoints
            long totalTime = end.timestamp() - start.timestamp();

            // must be ordered from earliest to latest
            double[] percentiles = {0.50, 0.90, 0.95, 0.99};

            for (double p : percentiles) {
                long targetTs = start.timestamp() + (long)(totalTime * p);
                try {
                    // Search for the offset that existed at this time
                    var offsetResult = admin.listOffsets(Map.of(tp, OffsetSpec.forTimestamp(targetTs)))
                            .all().get(timeoutMs(), TimeUnit.MILLISECONDS).get(tp);

                    if (offsetResult != null && offsetResult.timestamp() > 0 && offsetResult.offset() > start.offset()) {
                        history.addLast(new long[]{offsetResult.offset(), offsetResult.timestamp()});
                    }
                } catch (Exception ignored) {}
            }

            // Always end with the latest head
            if (end.offset() > start.offset()) {
                history.addLast(new long[]{end.offset(), end.timestamp()});
            }
        });
    }

    public void stop() {
        executor.shutdown();
    }

    private void refresh() {
        final long start = System.currentTimeMillis();

        listGroupIds()
                .<GroupSnapshot>thenCompose(ids -> {
                    var metadataFuture = fetchGroupMetadata(ids);
                    var offsetsFuture = fetchGroupOffsets(ids);

                    //return metadataFuture.thenCombine(offsetsFuture, GroupSnapshot::new);

                    return metadataFuture.thenCombine(offsetsFuture, (metadata, offsets) -> {
                        // 1. Reconcile metadata (remove groups that no longer exist)
                        this.groupMetadata.keySet().retainAll(metadata.keySet());
                        this.groupMetadata.putAll(metadata);

                        // 2. Reconcile offset history (remove groups that no longer exist)
                        this.offsetHistory.keySet().retainAll(metadata.keySet());

                        return new GroupSnapshot(metadata, offsets);
                    });
                })
                .thenCompose(snapshot -> {

                    //TODO what if scrapped at this time?  would be good to have clear/putAll be atomic.
                    //this.groupMetadata.clear();
                    //this.groupMetadata.putAll(snapshot.metadata());

                    // Calculate lag using the offsets from the snapshot
                    return calculateLag(snapshot.offsets());
                })
                .thenAccept(results -> {


                    // prune producer history on topic deleted
                    Set<TopicPartition> activePartitions = results.values().stream()
                            .flatMap(m -> m.keySet().stream())
                            .collect(Collectors.toSet());
                    this.producerHistory.keySet().retainAll(activePartitions);


                    //TODO what if scrapped at this time?  would be good to have clear/putAll be atomic.
                    groupLag.clear();

                    groupLag.putAll(results);

                    lastRefreshDurationMs = System.currentTimeMillis() - start;
                    refreshedAt = System.currentTimeMillis();

                })
                .exceptionally(ex -> {
                    log.error("Refresh failed", ex);
                    return null;
                });

        log.debug("refresh at={}, duration={}ms", Instant.ofEpochMilli(refreshedAt).atZone(ZoneId.systemDefault()).format(formatter), lastRefreshDurationMs);
    }


    private CompletableFuture<Map<String, Map<TopicPartition, LagDetail>>> calculateLag(Map<String, Map<TopicPartition, Long>> groupOffsets) {
        final Set<TopicPartition> allPartitions = groupOffsets.values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toSet());

        return fetchLatestOffsets(allPartitions).thenApply(latestOffsets -> {
            Map<String, Map<TopicPartition, LagDetail>> results = new TreeMap<>();
            long now = System.currentTimeMillis();

            // 1. Update Producer Timeline
            latestOffsets.forEach((tp, info) -> {
                var history = producerHistory.computeIfAbsent(tp, k -> new ArrayDeque<>());
                if (history.isEmpty() || history.peekLast()[0] != info.offset()) {
                    history.addLast(new long[]{info.offset(), info.timestamp()});
                    if (history.size() > 500) history.removeFirst(); // Increased for higher resolution
                }
            });

            groupOffsets.forEach((groupId, partitions) -> {
                Map<TopicPartition, LagDetail> groupResults = new TreeMap<>(Comparator.comparing(TopicPartition::topic).thenComparing(TopicPartition::partition));

                var groupHistoryMap = offsetHistory.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>());

                partitions.forEach((tp, committed) -> {
                    OffsetInfo latest = latestOffsets.get(tp);
                    if (latest != null) {
                        long offsetLag = Math.max(0, latest.offset() - committed);
                        Deque<long[]> window = groupHistoryMap.computeIfAbsent(tp, k -> new ArrayDeque<>());

                        if (window.isEmpty()) {
                            // INITIAL POLL: Record the offset but use -1 for timestamp to indicate "NA / Unknown"
                            // We don't know how long it's been at this offset before we started.
                            window.addLast(new long[]{committed, -1L});
                        } else if (window.peekLast()[0] != committed) {
                            // MOVEMENT DETECTED: This is a legitimate "Arrival" event.
                            window.addLast(new long[]{committed, now});
                            if (window.size() > 10) window.removeFirst();
                        }

                        // Interpolated (Broker Time)
                        long interpolatedTs = (offsetLag == 0) ? latest.timestamp() : interpolateTimestamp(tp, committed);

                        //System.out.println(interpolatedTs + " " + offsetLag);
                        // Observed (Local Time)
                        long observedTs = window.peekLast()[1];

                        groupResults.put(tp, new LagDetail(
                                latest.offset(),
                                latest.timestamp(),
                                committed,
                                interpolatedTs,
                                observedTs,
                                offsetLag,
                                velocity(window)
                        ));
                    }
                });
                results.put(groupId, groupResults);
            });
            return results;
        });
    }

    // ... existing code ...
    private long interpolateTimestamp(TopicPartition tp, long offset) {
        Deque<long[]> history = producerHistory.get(tp);

        if (history == null || history.isEmpty()) {
            return -1L;
        }

        // Case 1: Only one point. We can't determine a slope, so we return the best known timestamp.
        if (history.size() < 2) {
            return history.peekLast()[1];
        }

        long[] p1 = null;
        long[] p2 = null;

        // Iterate to find the segment [p1, p2] that brackets the offset
        for (long[] point : history) {
            if (point[0] <= offset) {
                p1 = point;
            } else {
                p2 = point;
                break;
            }
        }

        // Case 2: Bracket found (Offset is between two known points)
        if (p1 != null && p2 != null) {
            if (p2[0] == p1[0]) return p1[1];
            double ratio = (double) (offset - p1[0]) / (p2[0] - p1[0]);
            return p1[1] + (long) (ratio * (p2[1] - p1[1]));
        }

        // Case 3: Offset is beyond our latest point (Consumer is ahead of our last poll)
        // We use the slope of the NEWEST known segment to project forward.
        if (p1 != null) {
            long[] last = history.peekLast();
            long[] prev = null;
            var it = history.descendingIterator();
            it.next(); // skip last
            if (it.hasNext()) prev = it.next();

            if (prev != null && last[0] != prev[0]) {
                double ratio = (double) (offset - prev[0]) / (last[0] - prev[0]);
                return prev[1] + (long) (ratio * (last[1] - prev[1]));
            }
            return last[1];
        }

        // Case 4: Offset is before our oldest point (p1 is null)
        // Use the slope of the OLDEST known segment to project backward.
        long[] first = history.peekFirst();
        long[] second = null;
        var it = history.iterator();
        it.next(); // skip first
        if (it.hasNext()) second = it.next();

        if (second != null && second[0] != first[0]) {
            double ratio = (double) (offset - first[0]) / (second[0] - first[0]);
            return first[1] + (long) (ratio * (second[1] - first[1]));
        }

        return first[1];
    }
    // ... existing code ...

    private CompletableFuture<Map<TopicPartition, OffsetInfo>> fetchLatestOffsets(Set<TopicPartition> partitions) {

        final Map<TopicPartition, OffsetSpec> latestRequest = partitions.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        final Map<TopicPartition, OffsetSpec> maxTsRequest = partitions.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.maxTimestamp()));

        var latestFuture = admin.listOffsets(latestRequest).all().toCompletionStage().toCompletableFuture().orTimeout(timeoutMs(), TimeUnit.MILLISECONDS);
        var maxTsFuture = admin.listOffsets(maxTsRequest).all().toCompletionStage().toCompletableFuture().orTimeout(timeoutMs(), TimeUnit.MILLISECONDS);

        return latestFuture.thenCombine(maxTsFuture, (latestResult, maxTsResult) -> {
            long now = System.currentTimeMillis();
            return partitions.stream().collect(Collectors.toMap(
                    tp -> tp,
                    tp -> {
                        long hwm = latestResult.containsKey(tp) ? latestResult.get(tp).offset() : 0L;
                        long ts = maxTsResult.containsKey(tp) ? maxTsResult.get(tp).timestamp() : -1L;


                        // TODO 2000 a setting
                        // IMPROVEMENT: If the producer is active and the last message
                        // timestamp is very close to 'now' (e.g., within 2 seconds),
                        // we treat 'now' as the true head timestamp.
                        // This eliminates the "jitter" of the last batch's arrival time.
                        if (ts > 0 && (now - ts) < 2000) {
                            ts = now;
                        }

                        // if producer is sending timestamps in the future it is protected value of max future.
                        if (ts <= 0 || ts > (now + maxFutureDeltaMs)) {
                            ts = now;
                        }
                        return new OffsetInfo(hwm, ts);
                    }
            ));
        }).exceptionally(e -> {
            log.error("Failed to fetch latest offsets/timestamps", e);
            return Collections.emptyMap();
        });
    }


    private long calculateInterpolatedLag(TopicPartition tp, long committedOffset, OffsetInfo latest) {
        Deque<long[]> history = producerHistory.get(tp);

        System.out.println("HISTORY " + tp + " " + history.getLast());
        if (history == null || history.size() < 2) {
            return 0;
        }

        long[] p1 = null;
        long[] p2 = null;

        for (long[] point : history) {
            if (point[0] <= committedOffset) {
                p1 = point;
            } else {
                p2 = point;
                break;
            }
        }

        if (p1 != null && p2 != null) {
            // Linear interpolation using the Validated Timestamps from the history
            double ratio = (p2[0] == p1[0]) ? 0 : (double) (committedOffset - p1[0]) / (p2[0] - p1[0]);
            long estimatedTs = p1[1] + (long) (ratio * (p2[1] - p1[1]));

            // CRITICAL: Compare against the specific partition's latest timestamp, NOT System.now()
            return Math.max(0, latest.timestamp() - estimatedTs);
        } else if (p1 != null) {
            // If the consumer is at or past our latest known producer point
            return Math.max(0, latest.timestamp() - p1[1]);
        }

        return 0;
    }

    private CompletableFuture<List<String>> listGroupIds() {
        return admin.listGroups().all().toCompletionStage().toCompletableFuture()
                .orTimeout(timeoutMs(), TimeUnit.MILLISECONDS)
                .thenApply(groups -> groups.stream().map(GroupListing::groupId).toList())
                .exceptionally(e -> {
                    log.error("Failed to fetch consumer group IDs", e);
                    return Collections.emptyList();
                });
    }

    // Get the metadata for the groups
    // - number of members in the group
    // - state of the group (e.g., 'stable')
    //
    private CompletableFuture<Map<String, ConsumerGroupDescription>> fetchGroupMetadata(Collection<String> ids) {
        return admin.describeConsumerGroups(ids).all().toCompletionStage().toCompletableFuture();
    }

    // Get the offsets for the groups.
    //
    private CompletableFuture<Map<String, Map<TopicPartition, Long>>> fetchGroupOffsets(Collection<String> ids) {

        Map<String, ListConsumerGroupOffsetsSpec> specs = ids.stream()
                .collect(Collectors.toMap(id -> id, id -> new ListConsumerGroupOffsetsSpec()));

        return admin.listConsumerGroupOffsets(specs).all().toCompletionStage().toCompletableFuture()
                .thenApply(result -> result.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                m -> m.getValue().offset()
                        ))
                )));
    }

    public long lastRefreshDurationMs() {
        return lastRefreshDurationMs;
    }

    public long lastIntervalMs() {
        return lastIntervalMs;
    }

    private double velocity(Deque<long[]> window) {
        if (window.size() < 2) {
            return 0;
        }

        long[] newest = window.peekLast();
        long[] oldest = null;

        // Find the oldest valid entry (skipping the -1 initialization marker)
        for (long[] entry : window) {
            if (entry[1] != -1) {
                oldest = entry;
                break;
            }
        }


        // timeDeltaMs -> velocityMinWindowMs

        if (oldest != null && oldest != newest) {
            long offsetDelta = newest[0] - oldest[0];
            long timeDeltaMs = newest[1] - oldest[1];

            long requiredWindowMs = lastIntervalMs * config.getVelocityWindowMultiplier();

            if (timeDeltaMs >= requiredWindowMs && offsetDelta > 0) {
                return offsetDelta / (timeDeltaMs / 1000.0);
            }
        }

        return 0;
    }
}
