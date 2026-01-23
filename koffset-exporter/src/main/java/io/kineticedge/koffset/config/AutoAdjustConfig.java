package io.kineticedge.koffset.config;

public class AutoAdjustConfig {

    // enable autoadjustment - which is the default, so configuration needs
    // to set to false to disable auto-adjustmnets.
    private boolean enabled = true;

    // number of samples to use for interval calculation
    // recalculation occurs every N samples (and if outside of desired tolerance)
    private int samples = 4;

    // the tolerance allowed on interval calculation to avoid too aggressive recalculations
    private double tolerance = 0.10;

    // autorefresh cannot set a schedule below this interval; this prevents
    // a misconfigured primary scraper from causing koffset to capture metrics too frequently
    private long minRefreshMs = 2000L;

    //

    /* config loader */
    public AutoAdjustConfig() {
    }

    /* testing */
    public AutoAdjustConfig(boolean enabled, int samples, double tolerance, long minRefreshMs) {
        this.enabled = enabled;
        this.samples = samples;
        this.tolerance = tolerance;
        this.minRefreshMs = minRefreshMs;
    }

    //

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSamples() {
        return samples;
    }

    public void setSamples(int samples) {
        this.samples = samples;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public long getMinRefreshMs() {
        return minRefreshMs;
    }

    public void setMinRefreshMs(long minRefreshMs) {
        this.minRefreshMs = minRefreshMs;
    }

    //

    @Override
    public String toString() {
        return "AutoAdjustConfig{" +
                "enabled=" + enabled +
                ", samples=" + samples +
                ", tolerance=" + tolerance +
                ", minRefreshMs=" + minRefreshMs +
                '}';
    }
}
