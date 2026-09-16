package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.EnvConfigLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {

    // do not change defaults w/out thinking about them - as a change
    // to default could be a breaking change.
    @Test
    void testDefaults() {
        ServerConfig config = new ServerConfig();
        assertEquals(8080, config.getPort());
    }

    @Test
    void testConstructor() {
        ServerConfig config = new ServerConfig(9999);
        assertEquals(9999, config.getPort());
    }
    @Test
    void testGettersAndSetters() throws Exception{
        ServerConfig config = new ServerConfig();
        ConfigTestUtil.assertGettersAndSetters(new EnvConfigLoader(), config);
    }

    @Test
    void testToString() {
        ServerConfig config = new ServerConfig(1234);
        assertTrue(config.toString().contains("port=1234"));
    }

}