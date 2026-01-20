package io.kineticedge.koffset.config;

import java.util.HashMap;
import java.util.Map;

public class KoffsetConfig {

    private CollectorConfig collector = new CollectorConfig();
    private ServerConfig server = new ServerConfig();
    private AutoAdjustConfig autoAdjust = new AutoAdjustConfig();
    private Map<String, Object> kafka = new HashMap<>();

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
