package io.kineticedge.koffset.config;

import io.kineticedge.koffset.KafkaContainerTest;
import io.kineticedge.koffset.util.ConfigLoader;
import io.kineticedge.koffset.util.TestEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KoffsetConfigTest {

    @Test
    void testFromEnv() {

        final TestEnvironment env = new TestEnvironment();

        final ConfigLoader loader = new ConfigLoader(env);

        env.put("KOFFSET_AUTO_ADJUST_ENABLED", "false");
        env.put("KOFFSET_AUTO_ADJUST_MIN_SAMPLES", "10");
        env.put("KOFFSET_AUTO_ADJUST_TOLERANCE", "0.12");
        env.put("KOFFSET_AUTO_ADJUST_CADENCE_TOLERANCE", "999");
        env.put("KOFFSET_AUTO_ADJUST_COLLECTOR_MIN_REFRESH_MS", "2000");
        env.put("KOFFSET_SERVER_PORT", "9999");
        env.put("KOFFSET_SERVER_AUTOADJUST_MIN_SAMPLES", "20");
        env.put("KOFFSET_SERVER_AUTOADJUST_TOLERANCE", "0.80");
        env.put("KOFFSET_COLLECTOR_ADMIN_TIMEOUT", "11111");
        env.put("KOFFSET_COLLECTOR_DELAY", "123");
        env.put("KOFFSET_COLLECTOR_INTERVAL", "456");
        env.put("KOFFSET_COLLECTOR_HISTORY_SIZE", "999");
        env.put("KOFFSET_KAFKA_BOOTSTRAP_SERVERS", "abc");
        env.put("KOFFSET_KAFKA_FOO__BAR", "xyz");


        KoffsetConfig config = loader.parse(KoffsetConfig.class, "KOFFSET");

        System.out.println(config);
    }
}