package io.kineticedge.koffset.config;

public class CollectorConfig {

    // timeout used for making the kafka admin client calls
    private long adminTimeoutMs = 30_000L;

    // the delay before the first collection occurs
    private long initialDelayMs = 2_000L;

    // the interval used for collection, auto-adjuster (if enabled) will change this to align
    // with the primary scrape frequency.
    private long initialIntervalMs = 5_000L;

    private int velocityWindowMultiplier = 2;

    private int historySize = 500;

    private long freshnessThresholdMs = 200L;

    //

    /* config loader */
    public CollectorConfig() {
    }

    /* testing */
    public CollectorConfig(long adminTimeoutMs, long initialDelayMs, long initialIntervalMs, int historySize, long freshnessThresholdMs) {
        this.adminTimeoutMs = adminTimeoutMs;
        this.initialDelayMs = initialDelayMs;
        this.initialIntervalMs = initialIntervalMs;
        this.historySize = historySize;
        this.freshnessThresholdMs = freshnessThresholdMs;
    }

    //

    public long getAdminTimeoutMs() {
        return adminTimeoutMs;
    }

    public void setAdminTimeoutMs(long adminTimeoutMs) {
        this.adminTimeoutMs = adminTimeoutMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public long getInitialIntervalMs() {
        return initialIntervalMs;
    }

    public void setInitialIntervalMs(long initialIntervalMs) {
        this.initialIntervalMs = initialIntervalMs;
    }

    public int getVelocityWindowMultiplier() {
        return velocityWindowMultiplier;
    }

    public void setVelocityWindowMultiplier(int velocityWindowMultiplier) {
        this.velocityWindowMultiplier = Math.max(2, velocityWindowMultiplier);
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    public long getFreshnessThresholdMs() {
        return freshnessThresholdMs;
    }

    public void setFreshnessThresholdMs(long freshnessThresholdMs) {
        this.freshnessThresholdMs = freshnessThresholdMs;
    }

    //

    @Override
    public String toString() {
        return "CollectorConfig{" +
                "adminTimeout=" + adminTimeoutMs +
                ", delay=" + initialDelayMs +
                ", interval=" + initialIntervalMs +
                ", velocityWindowMultiplier=" + velocityWindowMultiplier +
                ", historySize=" + historySize +
                ", freshnessThresholdMs=" + freshnessThresholdMs +
                '}';
    }
}
