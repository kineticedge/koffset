package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.ConfigLoader;
import io.kineticedge.koffset.util.TestEnvironment;
import org.junit.jupiter.api.Test;

class AutoAdjustConfigTest {


    @Test
    void creation() {


        TestEnvironment env = new TestEnvironment();


        ConfigLoader loader = new ConfigLoader(env);

        env.put("KOFFSET_AUTO_ADJUST_ENABLED", "false");
        env.put("KOFFSET_AUTO_ADJUST_TOLERANCE", "0.60");
        env.put("KOFFSET_AUTO_ADJUST_MIN_SAMPLES", "99");
        env.put("KOFFSET_AUTO_ADJUST_CADENCE_TOLERANCE", "111");
        env.put("KOFFSET_KAFKA_BOOTSTRAP_SERVERS", "abc");

        KoffsetConfig config = loader.parse(KoffsetConfig.class, "KOFFSET");


        System.out.println("**");
        System.out.println(config.getAutoAdjust().isEnabled());
       // System.out.println(config.getAutoAdjust().getCadenceTolerance());
        System.out.println(config.getAutoAdjust().getTolerance());
        System.out.println(config.getAutoAdjust().getSamples());

        System.out.println(">>");
        System.out.println(config.getKafka());
    }
}