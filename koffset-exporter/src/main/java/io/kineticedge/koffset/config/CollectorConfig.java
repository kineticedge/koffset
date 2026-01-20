package io.kineticedge.koffset.config;

public class CollectorConfig {

    // timeout used for making the kafka admin client calls
    private long adminTimeout = 30_000L;

    // the delay before the first collection occurs
    private long initialDelay = 2_000L;

    // the interval used for collection, auto-adjuster (if enabled) will change this to align
    // with the primary scrape frequency.
    private long initialInterval = 5_000L;

    private int velocityWindowMultiplier = 2;

    private int historySize;

    private long freshnessThresholdMs = 200L;

    //

    /* config loader */
    public CollectorConfig() {
    }

    /* testing */
    public CollectorConfig(long adminTimeout, long initialDelay, long initialInterval, int historySize, long freshnessThresholdMs) {
        this.adminTimeout = adminTimeout;
        this.initialDelay = initialDelay;
        this.initialInterval = initialInterval;
        this.historySize = historySize;
        this.freshnessThresholdMs = freshnessThresholdMs;
    }

    //

    public long getAdminTimeout() {
        return adminTimeout;
    }

    public void setAdminTimeout(long adminTimeout) {
        this.adminTimeout = adminTimeout;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getInitialInterval() {
        return initialInterval;
    }

    public void setInitialInterval(long initialInterval) {
        this.initialInterval = initialInterval;
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
                "adminTimeout=" + adminTimeout +
                ", delay=" + initialDelay +
                ", interval=" + initialInterval +
                ", velocityWindowMultiplier=" + velocityWindowMultiplier +
                ", historySize=" + historySize +
                ", freshnessThresholdMs=" + freshnessThresholdMs +
                '}';
    }
}
