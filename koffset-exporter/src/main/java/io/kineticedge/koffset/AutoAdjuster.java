package io.kineticedge.koffset;

import io.kineticedge.koffset.config.AutoAdjustConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AutoAdjuster {

    private static final Logger log = LoggerFactory.getLogger(AutoAdjuster.class);

    private final AutoAdjustConfig config;
    private final LagAnalyzer lagAnalyzer;

    private final List<Long> scrapeTimestamps = new ArrayList<>();

    private boolean logged;

    //TODO is this config.getCadenceTolerance() ???
    private long minimalRefreshMs = 50L;

    public AutoAdjuster(final AutoAdjustConfig config, final LagAnalyzer lagAnalyzer) {
        this.config = config;
        this.lagAnalyzer = lagAnalyzer;
    }

    private long lastIntervalMs() {
        return lagAnalyzer.lastIntervalMs();
    }

    private long lastRefreshDurationMs() {
        return lagAnalyzer.lastRefreshDurationMs();
    }

    public void alignToScrape(long detectedIntervalMs) {

        if (!config.isEnabled()) {
            if (!logged) {
                log.info("auto-adjusting disabled.");
                logged = true;
            }
            return;
        }


        long targetInterval = detectedIntervalMs;

        if (detectedIntervalMs < minimalRefreshMs) {
            long multiplier = (long) Math.ceil((double) minimalRefreshMs / detectedIntervalMs);
            targetInterval = detectedIntervalMs * multiplier;
        }

        // --- FRESHNESS TOLERANCE CHECK ---
        // We want our data to be "fresh". If the time since our last refresh (dataAge)
        // is more than the allowed tolerance percentage of the interval, we adjust.
        long now = System.currentTimeMillis();
        long dataAge = now - lagAnalyzer.getRefreshedAt();
        double ageRatio = (double) dataAge / targetInterval;

        long safetyMargin = Math.min(500, (long) (targetInterval * 0.05));
        long maxAge = (long) (targetInterval * config.getTolerance());



        if (dataAge >= safetyMargin && dataAge <= maxAge) {
            log.debug("Data age {}ms is in Safe Zone ({}ms to {}ms). Skipping reschedule.",
                    dataAge, safetyMargin, maxAge);
            return;
        } else {
            log.debug("Data age {}ms is not in Safe Zone ({}ms to {}ms). Rescheduling.",
                    dataAge, safetyMargin, maxAge);

        }

//        if (ageRatio <= config.getTolerance()) {
//            log.debug("Data age {}ms is {}% of interval {}ms (within {}% tolerance). Skipping reschedule.",
//                    dataAge, (int)(ageRatio * 100), targetInterval, (int)(config.getTolerance() * 100));
//            return;
//        }

//        long currentInterval = lagAnalyzer.lastIntervalMs();
//        double drift = Math.abs(currentInterval - targetInterval) / (double) targetInterval;
//        if (drift <= config.getTolerance()) {
//            log.debug("drift={} Current cadence {}ms is within {}% tolerance of target {}ms. Skipping reschedule.", ((long) (drift * 10000)) / 10000.0,
//                    currentInterval, (int)(config.getTolerance() * 100), targetInterval);
//            return;
//        }

//        // --- FIX: Don't reschedule if we are already running at this frequency ---
//        // Allow a small drift (e.g., 500ms) to avoid jitter-induced resets
//        if (Math.abs(lastIntervalMs() - targetInterval) < 5) {
//            log.debug("Cadence stable at {}ms (target {}ms). Skipping reschedule.", lastIntervalMs(), targetInterval);
//            return;
//        }

       // long safetyMargin = Math.min(500, (long) (targetInterval * 0.05));
        long leadTimeMs = lastRefreshDurationMs() + safetyMargin;

        long delayMillis = targetInterval - leadTimeMs;
        if (delayMillis < 0) delayMillis = 0;

        log.info("Aligning scheduler: detected cadence {}ms, effective period {}ms. Next refresh in {}ms",
                detectedIntervalMs, lastIntervalMs(), delayMillis);

        lagAnalyzer.reschedule(targetInterval, delayMillis);
    }


    public void trackScrapeCadence() {
        long now = System.currentTimeMillis();
        synchronized (scrapeTimestamps) {
            scrapeTimestamps.add(now);
            if (scrapeTimestamps.size() > config.getSamples()) {
                scrapeTimestamps.removeFirst();
            }

            if (scrapeTimestamps.size() == config.getSamples()) {
                analyzeIntervals();
            }
        }
    }

    private void analyzeIntervals() {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < scrapeTimestamps.size(); i++) {
            intervals.add(scrapeTimestamps.get(i) - scrapeTimestamps.get(i - 1));
        }

        long average = intervals.stream().mapToLong(Long::longValue).sum() / intervals.size();
        boolean stable = intervals.stream().allMatch(interval ->
                Math.abs(interval - average) <= (average * config.getTolerance())
        );

        if (stable) {
            // To avoid being "overly aggressive", we only align if the average
            // is significantly different from our current track or if we've drifted.
            alignToScrape(average);
            //lagAnalyzer.alignToScrape(average);
            // Clear timestamps after adjustment to wait for a new stable window
            scrapeTimestamps.clear();
        }
    }

}
