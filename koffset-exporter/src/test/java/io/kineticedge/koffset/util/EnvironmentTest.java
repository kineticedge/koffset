package io.kineticedge.koffset.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvironmentTest {

    @Test
    void testWithRealImplementation() {
        Environment environment = new Environment();

        var map = environment.getAll();
        var entry = map.entrySet().iterator().next();

        Assertions.assertEquals(entry.getValue(), environment.get(entry.getKey()).get());
        Assertions.assertEquals(entry.getValue(), environment.get(entry.getKey(), "DEFAULT"));
        Assertions.assertEquals("DEFAULT", environment.get("__________________________________________", "DEFAULT"));
    }

    @Test
    void testWithTestImplementation() {
        TestEnvironment environment = new TestEnvironment();

        environment.put("A", "22");
        environment.put("C", "");
        environment.put("D", " ");
        environment.put("E", "FOO");

        assertEquals(22L, environment.getAsLong("A", 11L));
        assertEquals(100L, environment.getAsLong("B", 100L));
        assertEquals(101L, environment.getAsLong("C", 101L));
        assertEquals(102L, environment.getAsLong("D", 102L));
        assertEquals(103L, environment.getAsLong("E", 103L));
    }

}