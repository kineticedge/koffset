package io.kineticedge.koffset.util;

import io.kineticedge.koffset.util.domain.Foo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class EnvConfigLoaderTest {

    @Test
    void test() {

        TestEnvironment env = new TestEnvironment();

        env.put("X_STRING_VALUE", "_1");
        env.put("X_INTEGER_VALUE", "-27");
        env.put("X_LONG_VALUE", "9801");
        env.put("X_BOOLEAN_VALUE", "true");
        env.put("X_DOUBLE_VALUE", "1.23");
        env.put("X_BAR_STRING_VALUE", "_2");
        env.put("X_BAR_MAP_A", "value_a");
        env.put("X_BAR_MAP_b", "value_B");
        env.put("X_BAR_MAP_C_D", "value_c_d");
        env.put("X_BAR_MAP_A", "value_a");
        env.put("X_BAR_MAP_b", "value_B");
        env.put("X_BAR_MAP_C_D", "value_c_d");
        env.put("X_BAR_MAP_E__F", "value_e_f");
        env.put("X_BAR_MAP_G___H", "value_g_h");

        EnvConfigLoader loader = new EnvConfigLoader(env);

        Foo foo = new Foo();

        loader.populate(foo, "X");

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
                Map.entry("e_f", "value_e_f"),
                Map.entry("g__h", "value_g_h") // THIS IS BEHAVIOR, IS THIS WHAT WE WANT?
        ), foo.getBar().getMap());
    }

}


