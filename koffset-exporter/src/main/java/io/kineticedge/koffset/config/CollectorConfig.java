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

    //

    /* config loader */
    public CollectorConfig() {
    }

    /* property loader */
    public CollectorConfig(long adminTimeout, long initialDelay, long initialInterval, int historySize) {
        this.adminTimeout = adminTimeout;
        this.initialDelay = initialDelay;
        this.initialInterval = initialInterval;
        this.historySize = historySize;
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
        // TODO: can be removed once ConfigLoader is switched to using setter vs field reflection
        return Math.max(2, velocityWindowMultiplier);
    }

    public void setVelocityWindowMultiplier(int velocityWindowMultiplier) {
        // Enforce a minimum of 2 to prevent users from making things "bad"
        this.velocityWindowMultiplier = Math.max(2, velocityWindowMultiplier);
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
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
                '}';
    }
}
