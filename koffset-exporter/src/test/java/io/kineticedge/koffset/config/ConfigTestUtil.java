package io.kineticedge.koffset.config;

import io.kineticedge.koffset.util.ConfigLoader;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTestUtil {

    private static final Map<Class<?>, Object> TEST_VALUES = Map.of(
            String.class, "test-string",
            Integer.TYPE, 42,
            Integer.class, 43,
            Long.TYPE, 12345L,
            Long.class, 12346L,
            Boolean.TYPE, true,
            Boolean.class, false,
            Double.TYPE, 3.14,
            Double.class, 3.15
    );

    /**
     * Walks through all fields discovered by the ConfigLoader and verifies
     * that every property with a setter can be round-tripped.
     */
    public static void assertGettersAndSetters(ConfigLoader loader, Object bean) throws Exception{

        Method m = ConfigLoader.class.getDeclaredMethod("fields", Class.class, String.class);
        m.setAccessible(true);

        // We use the loader's own 'fields' logic to see what it 'sees'
        // This ensures the test validates exactly what the ConfigLoader will use.

        ((Stream<ConfigLoader.Field >) m.invoke(loader, bean.getClass(), "TEST")).forEach(field -> {

            if (field.setter() == null) {
                throw new RuntimeException("No setter found for field: " + field.name());
            }

            Object testValue = TEST_VALUES.get(field.returnType());

            if (testValue != null) {
                field.set(bean, testValue);
                assertEquals(testValue, field.get(bean), "Getter/Setter mismatch for field: " + field.name());
            } else if (Map.class.isAssignableFrom(field.returnType())) {
                Map<String, String> testMap = Map.of("key", "value");
                field.set(bean, testMap);
                assertEquals(testMap, field.get(bean));
            }
            // Add List/Set handling if needed...
        });
    }
}