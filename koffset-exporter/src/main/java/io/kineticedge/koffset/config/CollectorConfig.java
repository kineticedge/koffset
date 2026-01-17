package io.kineticedge.koffset.config;

public class CollectorConfig {

    private long adminTimeout;
    private long initialDelay = 2_000L;
    private long initialInterval = 5_000L;
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

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    @Override
    public String toString() {
        return "CollectorConfig{" +
                "adminTimeout=" + adminTimeout +
                ", delay=" + initialDelay +
                ", interval=" + initialInterval +
                ", historySize=" + historySize +
                '}';
    }
}
