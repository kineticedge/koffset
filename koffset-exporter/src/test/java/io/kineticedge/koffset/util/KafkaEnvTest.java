package io.kineticedge.koffset.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KafkaEnvTest {

    @Test
    void testToKafkaProperties() {

        TestEnvironment testEnvironment = new TestEnvironment();

        testEnvironment.put("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");

        KafkaEnv env = new KafkaEnv(testEnvironment);

        Map<String, Object> config = env.to("KAFKA_");

        assertEquals(
                Map.ofEntries(
                        Map.entry("bootstrap.servers", "localhost:9092")
                ),
                config
        );
    }
}