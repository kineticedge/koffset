package io.kineticedge.koffset.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class   ConfigLoader {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigLoader.class);

    // Interface Defaults
    private static final Map<Class<?>, Class<?>> INTERFACE_IMPLEMENTATIONS = Map.of(
            Map.class, java.util.HashMap.class,
            List.class, java.util.ArrayList.class,
            java.util.Set.class, java.util.HashSet.class
    );

    //

    @FunctionalInterface
    public interface PropertySetter {
        void set(Field field, Object object);
    }

    public record Field(String name, Class<?> returnType, Method getter, Method setter, String prefix, String sourceKey) {

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
                Class<?> typeToCreate = INTERFACE_IMPLEMENTATIONS.getOrDefault(returnType, returnType);
                final T obj = (T) typeToCreate.getDeclaredConstructor().newInstance();
                setter().invoke(object, obj);
                return obj;
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
                }
                return value;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //

    private final Map<Class<?>, PropertySetter> handlers = Map.ofEntries(
            Map.<Class<?>, PropertySetter>entry(String.class, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, s))),
            Map.<Class<?>, PropertySetter>entry(Integer.TYPE, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Integer.parseInt(s)))),
            Map.<Class<?>, PropertySetter>entry(Integer.class, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Integer.parseInt(s)))),
            Map.<Class<?>, PropertySetter>entry(Long.TYPE, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Long.parseLong(s)))),
            Map.<Class<?>, PropertySetter>entry(Long.class, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Long.parseLong(s)))),
            Map.<Class<?>, PropertySetter>entry(Boolean.TYPE, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Boolean.parseBoolean(s)))),
            Map.<Class<?>, PropertySetter>entry(Boolean.class, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Boolean.parseBoolean(s)))),
            Map.<Class<?>, PropertySetter>entry(Double.TYPE, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Double.parseDouble(s)))),
            Map.<Class<?>, PropertySetter>entry(Double.class, (m, o) -> getValue(m.sourceKey()).ifPresent(s -> m.set(o, Double.parseDouble(s)))),
            Map.<Class<?>, PropertySetter>entry(Map.class, (m, o) -> {
                final Map<String, Object> map = m.getOrCreate(o);
                final String prefix = m.sourceKey();
                final String filter = prefix + delimiter();
                getAllKeys().stream()
                        .filter(k -> k.startsWith(filter))
                        .forEach(k -> {
                            final String key = convertKey(k.substring(filter.length()));
                            getValue(k).ifPresent(v -> map.put(key, v));
                        });
            }),
            Map.entry(List.class, (m, o) -> {
                final List<Object> list = m.getOrCreate(o);
                final String prefix = m.sourceKey() + delimiter();

                // Scan for keys like PREFIX_0, PREFIX_1, etc.
                for (int i = 0; ; i++) {
                    final String indexedKey = prefix + i;
                    Optional<String> value = getValue(indexedKey);

                    if (value.isPresent()) {
                        list.add(value.get());
                    } else {
                        // Stop at the first missing index
                        break;
                    }
                }
            })
    );

    private String convertKey(String key) {
        final String d = String.valueOf(delimiter());
        final String dd = d + d;
        if (delimiter() != '.') {
            final String regex = "(?<!" + Pattern.quote(d) + ")" + Pattern.quote(d) + "(?!" + Pattern.quote(d) + ")";
            key = key.replaceAll(regex, ".");
        }
        return key.replace(dd, d).toLowerCase();
    }

    private PropertySetter findProcessor(Class<?> type, String basePackage) {
        if (handlers.containsKey(type)) {
            return handlers.get(type);
        }

        if (type.getName().startsWith(basePackage)) {
            return (m, o) -> {
                Object sub = m.getOrCreate(o);
                populate(sub, sub.getClass(), m.sourceKey() + delimiter());
            };
        }

        return null;
    }

    public ConfigLoader() {
    }

    public <T> void populate(final T object, String prefix) {

        if (!prefix.endsWith("" + delimiter())) {
            prefix += delimiter();
        }

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
                .filter(m -> !m.getName().equals("toString") &&
                        !m.getName().equals("getClass") &&
                        !m.getName().equals("hashCode") &&
                        !m.getName().equals("clone")
                )
                .filter(m -> m.getParameterCount() == 0)
                // supported types - primitives, Map, classes in same package ...
                .map(m -> {
                            String name = extractPropertyName(m.getName());
                            return new Field(
                                    name,
                                    m.getReturnType(),
                                    m,
                                    findSetter(clazz, name, m.getReturnType()).orElse(null),
                                    prefix,
                                    getKey(name, prefix)
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
                    log.error("Failed to process field {}", field.name());
                    throw new RuntimeException(e);
                }
            }
        });
    }


//    private static String getEnvironmentVariable(final String string, final String prefix) {
//        return prefix + PATTERN.matcher(string).replaceAll(match -> "_" + match.group()).toUpperCase();
//    }

    protected abstract char delimiter();

    /**
     * Translates a property name (e.g., "bootstrapServers") into a source-specific key (e.g., "KAFKA_BOOTSTRAP_SERVERS").
     */
    protected abstract String getKey(String name, String prefix);

    /**
     * Retrieves an optional value from the source (Env, Properties, etc.)
     */
    protected abstract Optional<String> getValue(String key);

    /**
     * Provides all keys available in the source (used for Map scanning).
     */
    protected abstract java.util.Set<String> getAllKeys();


}
