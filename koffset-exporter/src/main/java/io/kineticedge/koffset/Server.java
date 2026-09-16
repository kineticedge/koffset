package io.kineticedge.koffset;

import io.kineticedge.koffset.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(java.time.ZoneId.of("UTC"));

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final ServerConfig config;
    private final LagAnalyzer lagAnalyzer;
    private final AutoAdjuster autoAdjuster;

    public Server(final ServerConfig config, final LagAnalyzer lagAnalyzer, AutoAdjuster autoAdjuster) {
        this.config = config;
        this.lagAnalyzer = lagAnalyzer;
        this.autoAdjuster = autoAdjuster;
    }

    private Map<String, Map<TopicPartition, LagAnalyzer.LagDetail>> lag() {
        return lagAnalyzer.lag();
    }

    private Map<String, ConsumerGroupDescription> groupMetadata() {
        return lagAnalyzer.groupMetadata();
    }

    private long refreshedAt() {
        return lagAnalyzer.getRefreshedAt();
    }

    private long lastRefreshDurationMs() {
        return lagAnalyzer.lastRefreshDurationMs();
    }

    public void start() {

        // Daemon thread factories
        final ThreadFactory daemonFactory = r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        };

        if (Epoll.isAvailable()) {
            log.info("Using EpollEventLoopGroup");
            bossGroup = new MultiThreadIoEventLoopGroup(1, daemonFactory, EpollIoHandler.newFactory());
            workerGroup = new MultiThreadIoEventLoopGroup(4, daemonFactory, EpollIoHandler.newFactory());
        } else {
            log.info("Using NioEventLoopGroup");
            bossGroup = new MultiThreadIoEventLoopGroup(1, daemonFactory, NioIoHandler.newFactory());
            workerGroup = new MultiThreadIoEventLoopGroup(4, daemonFactory, NioIoHandler.newFactory());
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(65_536),
                                    new RequestHandler()
                            );
                        }
                    });

            ChannelFuture f = b.bind(config.getPort()).sync();
            serverChannel = f.channel();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close(); //async
            }
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
                bossGroup = null;
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
                workerGroup = null;
            }
        }
    }

    private class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {

            try {
                if (!req.decoderResult().isSuccess()) {
                    sendError(ctx, req, HttpResponseStatus.BAD_REQUEST);
                    return;
                }

                final String uri = req.uri();

                if (uri.startsWith("/metrics")) {
                    handleMetrics(ctx, req);
                } else if (uri.equals("/healthz")) {
                    sendResponse(ctx, req, HttpResponseStatus.OK, "OK");
                } else if (uri.equals("/readyz")) {
                    if (lagAnalyzer.getRefreshedAt() > 0) {
                        sendResponse(ctx, req, HttpResponseStatus.OK, "READY");
                    } else {
                        sendError(ctx, req, HttpResponseStatus.SERVICE_UNAVAILABLE, "Initializing");
                    }
                } else {
                    sendError(ctx, req, HttpResponseStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                sendError(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }

        }

        private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, String content) {
            ByteBuf buffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
            FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buffer);
            res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            finalizeAndWrite(ctx, req, res);
        }

        private void handleMetrics(ChannelHandlerContext ctx, FullHttpRequest req) {

            try {
                log.info("age of lagAnalyzer: {}ms", (System.currentTimeMillis() - lagAnalyzer.getRefreshedAt()));

                if (req.uri().contains("primary=true")) {
                    log.info("trackScrapeCadence:");
                    autoAdjuster.trackScrapeCadence();
                    //TODO - use autoAdjuster
                }

                // Allocate a buffer. Pooled is better if available.
                // We guess the size to avoid frequent resizing.
                ByteBuf buffer = ctx.alloc().buffer(1024 * 64);

                generateMetrics(buffer);

                FullHttpResponse res = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        buffer
                );

                res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; version=0.0.4; charset=utf-8");
                res.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
                finalizeAndWrite(ctx, req, res);
            } catch (Exception e) {
                sendError(ctx, req, HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
    }

    public void generateMetrics(io.netty.buffer.ByteBuf buffer) {

        writeTimestamp(buffer, "koffset_refreshed_ts", "", refreshedAt());
        writeSeconds(buffer, "koffset_refreshed_duration_seconds", "", lastRefreshDurationMs());
        writeSeconds(buffer, "koffset_refreshed_age_seconds", "", System.currentTimeMillis() - refreshedAt());


        final var groups = groupMetadata();
        final var lag = lag();

        writeLong(buffer, "koffset_number_of_groups", "", groups.size());
        writeLong(buffer, "koffset_number_of_group_partitions", "", lag().values().stream().flatMap(m -> m.keySet().stream()).distinct().count());

        groups.forEach((groupId, description) -> {

            String groupLabel = String.format("group=\"%s\"", groupId);

            writeLong(buffer,
                    "koffset_group_info",
                    String.format("group=\"%s\",state=\"%s\",coordinator=\"%d\"", groupId, description.groupState().name().toUpperCase(), description.coordinator().id()),
                    1);

            writeLong(buffer, "koffset_group_members", groupLabel, description.members().size());
            long membersWithAssignment = description.members().stream().filter(m -> m.assignment() != null && !m.assignment().topicPartitions().isEmpty()).count();
            writeLong(buffer, "koffset_group_members_assigned", groupLabel, membersWithAssignment);
        });

        lag.values().stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, _) -> existing,
                        () -> new TreeMap<>(Comparator.comparing(TopicPartition::topic).thenComparing(TopicPartition::partition))
                ))
                .forEach((tp, detail) -> {
                    String tpLabels = String.format("topic=\"%s\",partition=\"%d\"", tp.topic(), tp.partition());
                    writeLong(buffer, "koffset_latest_offset", tpLabels, detail.partitionHeadOffset());
                    writeTimestamp(buffer, "koffset_latest_offset_ts", tpLabels, detail.partitionHeadTimestamp());
                });

        lag.forEach((groupId, partitions) -> {
            partitions.forEach((tp, detail) -> {
                String labels = String.format("group=\"%s\",topic=\"%s\",partition=\"%d\"",
                        groupId, tp.topic(), tp.partition());

                // I don't need to record these for every group...
                // writeLong(buffer, "koffset_group_partition_head_offset", labels, detail.partitionHeadOffset());
                // writeTimestamp(buffer, "koffset_group_partition_head_ts", labels, detail.partitionHeadTimestamp());

                writeLong(buffer, "koffset_group_offset", labels, detail.groupCommittedOffset());
                writeTimestamp(buffer, "koffset_group_offset_interpolated_ts", labels, detail.groupOffsetInterpolatedTimestamp());
                writeTimestamp(buffer, "koffset_group_offset_observed_ts", labels, detail.groupOffsetFirstObservedTimestamp());

                writeLong(buffer, "koffset_group_lag", labels, detail.offsetLag());
                writeSeconds(buffer, "koffset_group_lag_seconds", labels, detail.partitionHeadTimestamp() - detail.groupOffsetInterpolatedTimestamp());

                writeLong(buffer, "koffset_group_lag", labels, detail.offsetLag());
                writeSeconds(buffer, "koffset_group_lag_seconds", labels, detail.partitionHeadTimestamp() - detail.groupOffsetInterpolatedTimestamp());

                // SMART STALENESS:
                // Measures how much "time" the consumer is behind the latest message in the log.
                // If both producer and consumer stop, this value remains stationary (accurate).
                // If the producer keeps moving but the consumer stops, this value increases (accurate).
                long stalenessMs = 0;
                if (detail.offsetLag() > 0) {
                    long logicalStaleness = detail.partitionHeadTimestamp() - detail.groupOffsetInterpolatedTimestamp();
                    // Grace period (2x refresh interval) to filter out natural commit latency.
                    if (logicalStaleness > (lagAnalyzer.lastIntervalMs() * 2)) {
                        stalenessMs = logicalStaleness;
                    }
                }
                writeSeconds(buffer, "koffset_group_offset_stale_seconds", labels, stalenessMs);

                writeLine(buffer, String.format("koffset_group_velocity_records_per_sec{%s} %.2f", labels, detail.groupVelocityRecordsPerSec()));

                // Optional: ETA (How many seconds until catch-up at current velocity)
                if (detail.groupVelocityRecordsPerSec() > 0 && detail.offsetLag() > 0) {
                    double eta = detail.offsetLag() / detail.groupVelocityRecordsPerSec();
                    writeLine(buffer, String.format("koffset_group_catchup_eta_seconds{%s} %.1f", labels, eta));
                }

            });
        });
    }

    private void writeLong(ByteBuf buffer, String metric, String labels, long value) {
        writeLine(buffer, String.format("%s{%s} %d", metric, labels, value));
    }

    private void writeTimestamp(ByteBuf buffer, String metric, String labels, long timestamp) {
        if (timestamp > 0) {
            writeLine(buffer, "# timestamp: " + formatter.format(Instant.ofEpochMilli(timestamp)));
            writeLine(buffer,
                    String.format("%s{%s} %.3f",
                            metric,
                            labels,
                            timestamp / 1000.0
                    )
            );
        } else {
            writeLine(buffer,
                    String.format("%s{%s} NaN",
                            metric,
                            labels
                    )
            );
        }
    }

    private void writeSeconds(ByteBuf buffer, String metric, String labels, long seconds) {
        writeLine(buffer,
                String.format("%s{%s} %.3f",
                        metric,
                        labels,
                        seconds / 1000.0
                )
        );
    }


    private void writeLine(io.netty.buffer.ByteBuf buffer, String line) {
        buffer.writeCharSequence(line, StandardCharsets.UTF_8);
        buffer.writeByte('\n');
    }

    private void finalizeAndWrite(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {

        boolean keepAlive = HttpUtil.isKeepAlive(req);
        if (keepAlive) {
            if (!req.protocolVersion().isKeepAliveDefault()) {
                res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
        } else {
            res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ChannelFuture f = ctx.writeAndFlush(res);
        if (!keepAlive) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendError(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status, String message) {
        String fullMessage = status.toString() + (message != null ? " (" + message + ")" : "");
        byte[] bytes = fullMessage.getBytes(StandardCharsets.UTF_8);

        FullHttpResponse res = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(bytes)
        );

        res.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());

        finalizeAndWrite(ctx, req, res);
    }

    private void sendError(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus status) {
        sendError(ctx, req, status, null);
    }

}