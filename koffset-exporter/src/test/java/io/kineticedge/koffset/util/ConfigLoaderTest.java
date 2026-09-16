package io.kineticedge.koffset.util;

import io.kineticedge.koffset.util.domain.Foo;
import io.kineticedge.koffset.util.domain.Bad;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.exceptions.misusing.MissingMethodInvocationException;

import java.util.List;
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
        properties.put("x.bar.something", "true");
        properties.put("x.baz.extra", "EXTRA!");
        properties.put("x.sub.field", "FIELD");
        properties.put("x.external.value", "IGNORED");

        properties.put("x.is", "IS");
        properties.put("x.set", "SET");
        properties.put("x.get", "GET");

        TestEnvironment env = new TestEnvironment();
        env.put("X_STRING_VALUE", "string_env");
        env.put("X_LONG_VALUE", "555");
        env.put("X_BAR_MAP_A", "environment_a");
        env.put("X_BAR_MAP_c", "environment_c");
        env.put("X_BAZ_LIST_0", "zero");
        env.put("X_BAZ_LIST_1", "one");
        env.put("X_BAZ_LIST_3", "three"); // a skipped element is not populated, list ends when list is not sequential

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
        Assertions.assertTrue(foo.getBar().isSomething());
        Assertions.assertEquals("EXTRA!", foo.getBaz().extra());
        Assertions.assertEquals(List.of("zero", "one"), foo.getBaz().list());
        Assertions.assertEquals("FIELD", foo.getSub().getField());
        Assertions.assertNull(foo.getExternal());

        Assertions.assertEquals("IS", foo.is());
        Assertions.assertEquals("SET", foo.set());
        Assertions.assertEquals("GET", foo.get());

    }

    @Test
    void errorPossibilities() {

        Properties properties = new  Properties();

        final ConfigLoader loader = new PropertyConfigLoader(properties);

        final Bad foo = new Bad();

        properties.clear();
        properties.put("x.missing.setter", "VALUE");
        Assertions.assertThrows(RuntimeException.class, () -> loader.populate(foo, "x."));

        // if error is through on a primitive getter, it will not be called, since primitives do not involve the getter method, the getter method
        // is to determine the name of the property, it is only called on non-primitives
        properties.clear();
        properties.put("x.bad.get", "VALUE");
        loader.populate(foo, "x");
        Assertions.assertDoesNotThrow(() -> loader.populate(foo, "x"));

        properties.clear();
        properties.put("x.bad.set", "VALUE");
        Assertions.assertThrows(RuntimeException.class, () -> loader.populate(foo, "x"));

        properties.clear();
        properties.put("x.non.public.setter", "NON_PUBLIC_SETTER");
        Assertions.assertThrows(RuntimeException.class, () -> loader.populate(foo, "x"));

        properties.clear();
        properties.put("x.non.public.getter", "NON_PUBLIC_GETTER");
        Assertions.assertDoesNotThrow(() -> loader.populate(foo, "x"));
    }
}


