package io.kineticedge.koffset.util.old;

import io.kineticedge.koffset.config.KoffsetConfig;
import io.kineticedge.koffset.util.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ConfigLoader2 {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigLoader2.class);

    // use to break apart properCase variable into words
    private static final Pattern PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

    //

    @FunctionalInterface
    public interface PropertySetter {
        void set(Field field, Object object);
    }

    public record Field(String name, Class<?> returnType, Method getter, Method setter, String prefix) {

        @SuppressWarnings("unchecked")
        public <T> T get(Object object) {
            try {
                return (T) getter().invoke(object);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> void set(Object object, T value) {
            try {
                setter().invoke(object, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T create(Object object) {
            try {
                return (T) setter().invoke(object, returnType.getDeclaredConstructor().newInstance());
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T getOrCreate(Object object) {
            try {
                T value = (T) getter().invoke(object);
                if (value == null) {
                    value = create(object);
                    if (setter() != null) {
                        setter().invoke(object, value);
                    } else {
                        throw new IllegalStateException("No setter available for field: " + name());
                    }
                }
                return value;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

       public String envVar() {
            return getEnvironmentVariable(name(), prefix());
        }

    }


    //

    private Environment env;

    private final Map<Class<?>, PropertySetter> handlers = Map.ofEntries(
            Map.<Class<?>, PropertySetter>entry(String.class, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, s))),
            Map.<Class<?>, PropertySetter>entry(Integer.TYPE, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Integer.parseInt(s)))),
            Map.<Class<?>, PropertySetter>entry(Integer.class, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Integer.parseInt(s)))),
            Map.<Class<?>, PropertySetter>entry(Long.TYPE, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Long.parseLong(s)))),
            Map.<Class<?>, PropertySetter>entry(Long.class, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Long.parseLong(s)))),
            Map.<Class<?>, PropertySetter>entry(Boolean.TYPE, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Boolean.parseBoolean(s)))),
            Map.<Class<?>, PropertySetter>entry(Boolean.class, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Boolean.parseBoolean(s)))),
            Map.<Class<?>, PropertySetter>entry(Double.TYPE, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Double.parseDouble(s)))),
            Map.<Class<?>, PropertySetter>entry(Double.class, (m, o) -> env.get(m.envVar()).ifPresent(s -> m.set(o, Double.parseDouble(s)))),
            Map.<Class<?>, PropertySetter>entry(Map.class, (m, o) -> {
                final Map<String, Object> map = m.getOrCreate(o);
                final String prefix = m.envVar();
                final String filter = prefix + "_";
                env.getAll().keySet().stream()
                        .filter(k -> k.startsWith(filter))
                        .forEach(k -> {
                            String key = k.substring(filter.length())
                                    .replaceAll("(?<!_)_(?!_)", ".")
                                    .replaceAll("__", "_")
                                    .toLowerCase();
                            env.get(k).ifPresent(v -> map.put(key, v));
                        });
            })
    );

    private PropertySetter findProcessor(Class<?> type, String basePackage) {
        if (handlers.containsKey(type)) {
            return handlers.get(type);
        }

        if (type.getName().startsWith(basePackage)) {
            return (m, o) -> {
                Object sub = m.getOrCreate(o);
                if (sub != null) {
                    populate(sub, sub.getClass(), m.envVar() + "_");
                }
            };
        }

        return null;
    }

    public ConfigLoader2() {
        this.env = new Environment();
    }

    // exposed for testing
    public ConfigLoader2(Environment env) {
        this.env = env;
    }

    public <T> void populate(final T object, final String prefix) {
        populate(object, object.getClass(), prefix);
    }

    private String extractPropertyName(String methodName) {
        if ((methodName.startsWith("get") || methodName.startsWith("set")) && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return methodName;
    }

    private Optional<Method> findSetter(Class<?> clazz, String propertyName, Class<?> type) {
        String capitalized = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        // with a given fieldName, derived from a getter is 'fieldName()', 'getFieldName()', 'isFieldName()', a setter can be 'fieldName()' or 'setFieldName()'.
        // the type for the getter and setter must match exactly, this means that a getter cannot use LinkedHashMap and the setter use HashMap.
        return Stream.of("set" + capitalized, propertyName)
                .map(name -> {
                    try {
                        Method m = clazz.getMethod(name, type);
                        return Modifier.isPublic(m.getModifiers()) ? m : null;
                    } catch (NoSuchMethodException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private Stream<Field> fields(Class<?> clazz, String prefix) {
        return Stream.of(clazz.getDeclaredMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.getName().equals("toString")) // any other "gotchas"?
                .filter(m -> m.getParameterCount() == 0)
                // supported types - primitives, Map, classes in same package ...
                .map(m -> {
                            String name = extractPropertyName(m.getName());
                            return new Field(
                                    name,
                                    m.getReturnType(),
                                    m,
                                    findSetter(clazz, name, m.getReturnType()).orElse(null),
                                    prefix
                            );
                        }
                );
    }

    //.collect(Collectors.groupingBy(Field::returnType));

    private <T> void populate(final T object, Class<?> clazz, final String prefix) {
        String basePackage = object.getClass().getPackageName();

        fields(clazz, prefix).forEach(field -> {
            PropertySetter processor = findProcessor(field.returnType(), basePackage);
            if (processor != null) {
                try {
                    processor.set(field, object);
                } catch (Exception e) {
                    log.debug("Failed to process field {}", field.name());
                }
            }
        });
    }


    private static String getEnvironmentVariable(final String string, final String prefix) {
        return prefix + PATTERN.matcher(string).replaceAll(match -> "_" + match.group()).toUpperCase();
    }

    //

    public static class TestEnvironment extends Environment {

        Map<String, String> env = new HashMap<>();

        @Override
        public Map<String, String> getAll() {
            return env;
        }

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(env.get(key));
        }

        // testing 'hooks'

        public void put(String key, String value) {
            env.put(key, value);
        }

        public void clear() {
            env.clear();
        }

    }

    public void main(String[] args) {
        TestEnvironment e = new TestEnvironment();
        e.put("KOFFSET_KAFKA_BOOTSTRAP_SERVERS", "a");
        e.put("KOFFSET_COLLECTOR_ADMIN_TIMEOUT", "555");
        e.put("KOFFSET_COLLECTOR", "555");
        ConfigLoader2 config = new ConfigLoader2(e);
        KoffsetConfig koffsetConfig = new KoffsetConfig();
        config.populate(koffsetConfig, "KOFFSET_");
        System.out.println(koffsetConfig);
    }

}
