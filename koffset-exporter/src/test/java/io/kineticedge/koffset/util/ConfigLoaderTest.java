package io.kineticedge.koffset.util;

import io.kineticedge.koffset.util.domain.Foo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

class ConfigLoaderTest {

    /**
     * This tests shows how properties can be stacked on each other
     * This does mean maps are merged (not replaced)
     */
    @Test
    void test() {
        Properties properties = new Properties();
        properties.put("x.string.value", "string_property");
        properties.put("x.integer.value", "22");
        properties.put("x.bar.map.a", "property_a");
        properties.put("x.bar.map.B", "property_b");

        TestEnvironment env = new TestEnvironment();
        env.put("X_STRING_VALUE", "string_env");
        env.put("X_LONG_VALUE", "555");
        env.put("X_BAR_MAP_A", "environment_a");
        env.put("X_BAR_MAP_c", "environment_c");

        ConfigLoader propertyLoader = new PropertyConfigLoader(properties);
        ConfigLoader envLoader = new EnvConfigLoader(env);

        Foo foo = new Foo();

        propertyLoader.populate(foo, "x");
        envLoader.populate(foo, "X");

        Assertions.assertEquals("string_env", foo.getStringValue());
        Assertions.assertEquals(22, foo.getIntegerValue());
        Assertions.assertEquals(555, foo.getLongValue());
        Assertions.assertEquals(Map.ofEntries(
                Map.entry("a", "environment_a"),
                Map.entry("b", "property_b"),
                Map.entry("c", "environment_c")
        ), foo.getBar().getMap());
    }

}


