package io.kineticedge.koffset.util;

import io.kineticedge.koffset.util.domain.Foo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

public class PropertyConfigLoaderTest {

    @Test
    void test() {

        Properties properties = new Properties();

        properties.put("x.string.value", "_1");
        properties.put("x.integer.value", "-27");
        properties.put("x.long.value", "9801");
        properties.put("x.boolean.value", "true");
        properties.put("x.double.value", "1.23");
        properties.put("x.bar.string.value", "_2");
        properties.put("x.bar.map.a", "value_a");
        properties.put("x.bar.map.B", "value_B");
        properties.put("x.bar.map.c.d", "value_c_d");
        properties.put("x.bar.map.e..f", "value_e_f");
        properties.put("x.bar.map.g...h", "value_g_h");


        PropertyConfigLoader loader = new PropertyConfigLoader(properties);

        Foo foo = new Foo();

        loader.populate(foo, "x");

        Assertions.assertEquals("_1", foo.getStringValue());
        Assertions.assertEquals(-27, foo.getIntegerValue());
        Assertions.assertEquals(9801, foo.getLongValue());
        Assertions.assertEquals(true, foo.getBooleanValue());
        Assertions.assertEquals(1.23, foo.getDoubleValue());
        Assertions.assertEquals("_2", foo.getBar().getStringValue());

        Assertions.assertEquals(Map.ofEntries(
                Map.entry("a", "value_a"),
                Map.entry("b", "value_B"),
                Map.entry("c.d", "value_c_d"),
                Map.entry("e.f", "value_e_f"),
                Map.entry("g..h", "value_g_h") // THIS IS BEHAVIOR, IS THIS WHAT WE WANT?
        ), foo.getBar().getMap());

    }

}
