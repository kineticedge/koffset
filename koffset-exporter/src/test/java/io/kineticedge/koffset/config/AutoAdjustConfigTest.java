package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.EnvConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoAdjustConfigTest {

    // do not change defaults w/out thinking about them - as a change
    // to default could be a breaking change.
    @Test
    void testDefaults() {
        AutoAdjustConfig config = new AutoAdjustConfig();
        assertTrue(config.isEnabled()); // changing this is a definately breaking contract, needs to be a major revision.
        assertEquals(2000L, config.getMinRefreshMs());
        assertEquals(4, config.getSamples());
        assertEquals(0.10, config.getTolerance());
    }


    @Test
    void testConstructor() {
        AutoAdjustConfig config = new AutoAdjustConfig(false, 10, 0.99, 123L);

        assertFalse(config.isEnabled());
        assertEquals(123L, config.getMinRefreshMs());
        assertEquals(10, config.getSamples());
        assertEquals(0.99, config.getTolerance());

    }

    @Test
    void testGettersAndSetters() throws Exception{
        AutoAdjustConfig config = new AutoAdjustConfig();
        ConfigTestUtil.assertGettersAndSetters(new EnvConfigLoader(), config);
    }
}