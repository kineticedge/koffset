package io.kineticedge.koffset.config;

public class AutoAdjustConfig {

    private boolean enabled = true;
    private int samples = 4;
    private double tolerance = 0.10;
    private long cadenceTolerance = 500;

    //

    /* config loader */
    public AutoAdjustConfig() {
    }

    /* property loader */
    public AutoAdjustConfig(boolean enabled, int samples, double tolerance, long cadenceTolerance) {
        this.enabled = enabled;
        this.samples = samples;
        this.tolerance = tolerance;
        this.cadenceTolerance = cadenceTolerance;
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

    public long getCadenceTolerance() {
        return cadenceTolerance;
    }

    public void setCadenceTolerance(long cadenceTolerance) {
        this.cadenceTolerance = cadenceTolerance;
    }

    @Override
    public String toString() {
        return "AutoAdjustConfig{" +
                "enabled=" + enabled +
                ", samples=" + samples +
                ", tolerance=" + tolerance +
                ", cadenceTolerance=" + cadenceTolerance +
                '}';
    }
}
