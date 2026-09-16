package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.EnvConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectorConfigTest {

    // do not change defaults w/out thinking about them - as a change
    // to default could be a breaking change.
    @Test
    void testDefaults() {
        CollectorConfig config = new CollectorConfig();
        assertEquals(30_000L, config.getAdminTimeoutMs());
        assertEquals(500, config.getHistorySize());
        assertEquals(2_000L, config.getInitialDelayMs());
        assertEquals(5_000L, config.getInitialIntervalMs());
    }

    @Test
    void testConstructor() {
        CollectorConfig config = new CollectorConfig(123L, 789L, 101112, 456, 200L);
        assertEquals(123L, config.getAdminTimeoutMs());
        assertEquals(789L, config.getInitialDelayMs());
        assertEquals(101112L, config.getInitialIntervalMs());
        assertEquals(456, config.getHistorySize());
        assertEquals(200L, config.getFreshnessThresholdMs());
    }

    @Test
    void testGettersAndSetters() throws Exception{
        CollectorConfig config = new CollectorConfig();
        ConfigTestUtil.assertGettersAndSetters(new EnvConfigLoader(), config);
    }

    @Test
    void testToString() {
        CollectorConfig config = new CollectorConfig(123L, 789L, 101112, 456, 200L);
        assertTrue(config.toString().contains("freshnessThresholdMs=200"));
    }
}