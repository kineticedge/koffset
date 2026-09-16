package io.kineticedge.koffset.config;

import java.util.HashMap;
import java.util.Map;

public class KoffsetConfig {

    private CollectorConfig collector;
    private ServerConfig server;
    private AutoAdjustConfig autoAdjust;
    private Map<String, Object> kafka;

    //

    public KoffsetConfig() {
    }

    //

    public CollectorConfig getCollector() {
        return collector;
    }

    public void setCollector(CollectorConfig collector) {
        this.collector = collector;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public AutoAdjustConfig getAutoAdjust() {
        return autoAdjust;
    }

    public void setAutoAdjust(AutoAdjustConfig autoAdjust) {
        this.autoAdjust = autoAdjust;
    }

    public Map<String, Object> getKafka() {
        return kafka;
    }


    public void setKafka(Map<String, Object> kafka) {
        this.kafka = kafka;
    }

    //

    @Override
    public String toString() {
        return "KoffsetConfig{" +
                "collector=" + collector +
                ", server=" + server +
                ", autoAdjust=" + autoAdjust +
                ", kafka=" + kafka +
                '}';
    }
}
