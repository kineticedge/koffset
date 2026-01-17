package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.ConfigLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// configured through environment
//
// Kafka Admin Client configured via "KOFFSET_KAFKA_" prefix
//
// KOFFSET_AUTOADJUST_ENABLED = true
// KOFFSET_AUTOADJUST_MIN_SAMPLES = 4
// KOFFSET_AUTOADJUST_CADENCE_TOLERANCE = 0.10
// KOFFSET_AUTOADJUST_COLLECTOR_MIN_REFRESH_MS = 500L
//
// KOFFSET_SERVER_PORT = 8080
// KOFFSET_SERVER_AUTOADJUST_MIN_SAMPLES = 4
// KOFFSET_SERVER_AUTOADJUST_TOLERANCE = 0.10
//
// KOFFSET_COLLECTOR_ADMIN_TIMEOUT = 30_000L
// KOFFSET_COLLECTOR_DELAY
// KOFFSET_COLLECTOR_INTERVAL
// KOFFSET_COLLECTOR_HISTORY_SIZE = 500
//
public class KoffsetConfig {

    private CollectorConfig collector = new CollectorConfig();
    private ServerConfig server = new ServerConfig();
    private AutoAdjustConfig autoAdjust = new AutoAdjustConfig();
    private Map<String, Object> kafka = new HashMap<>();

    //

    /* config loader */
    public KoffsetConfig() {
    }

    /* property loader */
    public KoffsetConfig(CollectorConfig collector, ServerConfig server, AutoAdjustConfig autoAdjust, Map<String, Object> kafka) {
        this.collector = collector;
        this.server = server;
        this.autoAdjust = autoAdjust;
        this.kafka = kafka;
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
