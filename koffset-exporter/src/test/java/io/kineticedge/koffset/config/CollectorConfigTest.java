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
        assertEquals(30_000L, config.getAdminTimeout());
        assertEquals(0, config.getHistorySize());
        assertEquals(2_000L, config.getInitialDelay());
        assertEquals(5_000L, config.getInitialInterval());
    }

    @Test
    void testConstructor() {
        CollectorConfig config = new CollectorConfig(123L, 789L, 101112, 456, 200L);
        assertEquals(123L, config.getAdminTimeout());
        assertEquals(789L, config.getInitialDelay());
        assertEquals(101112L, config.getInitialInterval());
        assertEquals(456, config.getHistorySize());
        assertEquals(200L, config.getFreshnessThresholdMs());
    }

    @Test
    void testGettersAndSetters() throws Exception{
        CollectorConfig config = new CollectorConfig();
        ConfigTestUtil.assertGettersAndSetters(new EnvConfigLoader(), config);
    }
}