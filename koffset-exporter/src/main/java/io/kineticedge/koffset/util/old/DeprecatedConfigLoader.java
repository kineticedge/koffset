package io.kineticedge.koffset.util.old;

import io.kineticedge.koffset.util.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class DeprecatedConfigLoader {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeprecatedConfigLoader.class);

    // use to break apart properCase variable into words
    private static final Pattern PATTERN = Pattern.compile("(?<=[a-z])[A-Z]");

    private Environment env;

    public DeprecatedConfigLoader() {
        this.env = new Environment();
    }

    // exposed for testing
    public DeprecatedConfigLoader(Environment env) {
        this.env = env;
    }

    public <T> T parse(final Class<T> clazz, String prefix) {

        if (!prefix.endsWith("_")) {
            prefix += "_";
        }

        final T options = create(clazz);

        populateByEnvironment(options, prefix);

        return options;
    }

    private static <T> T create(final Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> void populateByEnvironment(final T object, final String prefix) {
        Class<?> clazz = object.getClass();
        while (!clazz.equals(Object.class)) {
            populateByEnvironment(object, clazz, prefix);
            clazz = clazz.getSuperclass();
        }
    }

//    private <T> void populateBySetterMethods(final T object, final Class<?> clazz, final String prefix) {
//
//        log.debug("loading config class {}", clazz.getName());
//
//        Stream.of(clazz.getDeclaredMethods())
//                .filter(m -> !Modifier.isStatic(m.getModifiers()))
//                .filter(m -> Modifier.isPublic(m.getModifiers()))
//                .filter(m -> m.getParameterCount() == 1)
//                .forEach(m -> {
//                    final String name = extractName(m.getName());
//                    final Class<?> type = m.getParameters()[0].getType();
//
//                    final String environment = getEnvironmentVariable(name, prefix);
//
//
////                    if (m.getReturnType().getName().startsWith("io.kineticedge.koffset.config")) {
////                        try {
////                            Object subobject = m.get(object);
////
////                            System.out.println("A");
////                            if (subobject == null) {
////                                System.out.println("B");
////                                subobject = create(f.getType());
////                                f.set(object, subobject);
////                            }
////
////                            populateByEnvironment(subobject, f.getType(), environment + "_");
////                        } catch (IllegalAccessException e) {
////                            throw new RuntimeException(e);
////                        }
////                        return;
////                    }
//
//
//                    final String value = env.get(environment);
//
//                    log.info("using env={} for method={}({})", environment, m.getName(), type.getSimpleName());
//
//
////                    if (Map.class.equals(f.getType())) {
////
////                        //TODO use KafkaEnv or remove KafkaEnv...
////                        String pp = environment + "_";
////
////                        f.setAccessible(true);
////                        final String p = environment + "_";
////                        final List<String> overrides = env.getAll().keySet().stream().filter(e -> e.startsWith(pp)).toList();
////                        if (!overrides.isEmpty()) {
////                            final Map<String, Object> map = getMap(object, f);
////                            overrides.forEach(e -> {
////                                final String key = e.substring(prefix.length()).replaceAll("(?<!_)_(?!_)", ".").replaceAll("__", "_").toLowerCase();
////                                //String key = e.substring(prefix.length()).toLowerCase().replace("_", ".");
////                                map.put(key, env.get(e));
////                            });
////                        }
////                    }
//
//
//                    if (value != null) {
//                        try {
//                            log.debug("setting field={} type={} env={}", m.getName(), type.getSimpleName(), environment);
//                            if (String.class.equals(type)) {
//                                m.invoke(object, value);
//                            } else if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
//                                m.invoke(object, toBoolean(value));
//                            } else if (Integer.TYPE.equals(type) || Integer.class.equals(type)) {
//                                m.invoke(object, Integer.parseInt(value));
//                            } else if (Long.TYPE.equals(type) || Long.class.equals(type)) {
//                                m.invoke(object, Long.parseLong(value));
//                            } else if (Double.TYPE.equals(type) || Double.class.equals(type)) {
//                                m.invoke(object, Double.parseDouble(value));
//                            } else if (Float.TYPE.equals(type) || Float.class.equals(type)) {
//                                m.invoke(object, Float.parseFloat(value));
//                            } else if (List.class.equals(type)) {
//                                // only list of strings are supported, and delimiter must be ","
//                                m.invoke(object, Arrays.asList(value.trim().split("\\s*,\\s*")));
//                            } else if (Map.class.equals(type)) {
//                                // if the field type is a Map, assuming input is a key=value list of properties.
//                                m.invoke(object, toMap(value));
//                            } else if (Enum.class.isAssignableFrom(type)) {
//                                m.invoke(object, create(type, value));
//                            } else if (Duration.class.equals(type)) {
//                                m.invoke(object, Duration.parse(value));
//                            } else if (File.class.equals(type)) {
//                                m.invoke(object, new File(value));
//                            } else if (!type.isPrimitive()) {
//                                // handles any class where a constructor that takes a string is available.
//                                try {
//                                    final Constructor<?> constructor = type.getConstructor(String.class);
//                                    m.invoke(object, constructor.newInstance(value));
//                                } catch (final InstantiationException | NoSuchMethodException | InvocationTargetException e) {
//                                    // if this gets executed, either implement support or change your configuration class to have a different type.
//                                    String msg = String.format("field=%s (%s), unsupported type=%s value not set.", m.getName(), environment, type);
//                                    throw new RuntimeException(msg, e);
//                                }
//                            } else {
//                                // if this gets executed, either implement support or change your configuration class to have a different type.
//                                String msg = String.format("field=%s (%s), unsupported type=%s value not set.", m.getName(), environment, type);
//                                throw new RuntimeException(msg);
//                            }
//                        } catch (final InvocationTargetException | IllegalAccessException e) {
//                            // because we are using reflection, these have to be catched. However, since we are only accessing
//                            // public accessible methods, these should not happen.
//                            throw new RuntimeException(e);
//                        }
//                    }
//                });
//    }

    private <T> void populateByEnvironment(final T object, final Class<?> clazz, final String prefix) {

        Stream.of(clazz.getDeclaredFields()).forEach(f -> {

            f.setAccessible(true);

            final String environment = getEnvironmentVariable(f.getName(), prefix);
            final String propertyName = getPropertyKey(f.getName(), prefix);

//            System.out.println(f.getType());
//            System.out.println("env " + environment);
//            System.out.println("pr  " + propertyName);

            if (f.getType().getName().startsWith("io.kineticedge.koffset.config")) {
                try {
                    Object subobject = f.get(object);
                    if (subobject == null) {
                        subobject = create(f.getType());
                        f.set(object, subobject);
                    }
                    populateByEnvironment(subobject, f.getType(), environment + "_");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return;
            }


            System.out.println(environment);
            // by using 'env' instead of System.getEnvironment, testing becomes easier.
            final String value = env.get(environment).orElse(null);

            // process any individually defined property for a given map, e.g. if a map is "FOO" then any property can be
            // added to that map individually with FOO__BAR=27. It assumes that these properties are going to Kafka, so
            // they are converted to lower case and '_' replaced with '.'.
            if (Map.class.equals(f.getType())) {

                //TODO use KafkaEnv or remove KafkaEnv...
                String pp = environment + "_";

                f.setAccessible(true);
               // final String p = environment + "_";
                final List<String> overrides = env.getAll().keySet().stream().filter(e -> e.startsWith(pp)).toList();

                System.out.println(overrides);
                if (!overrides.isEmpty()) {
                    final Map<String, Object> map = getMap(object, f);
                    overrides.forEach(e -> {
                        final String key = e.substring(pp.length()).replaceAll("(?<!_)_(?!_)", ".").replaceAll("__", "_").toLowerCase();
                        //String key = e.substring(prefix.length()).toLowerCase().replace("_", ".");

                        System.out.println("env " + e);
                        System.out.println("PR " + getPropertyKey(e, ""));
//                        System.out.println("e : " + e);
//                        System.out.println("KEY : " + key);
                        map.put(key, env.get(e));
                    });
                }
            }


            if (value != null) {
                try {
//                    f.setAccessible(true);
                    if (String.class.equals(f.getType())) {
                        f.set(object, value);
                    } else if (Boolean.TYPE.equals(f.getType()) || Boolean.class.equals(f.getType())) {
                        f.set(object, toBoolean(value));
                    } else if (Integer.TYPE.equals(f.getType()) || Integer.class.equals(f.getType())) {
                        f.set(object, Integer.parseInt(value));
                    } else if (Long.TYPE.equals(f.getType()) || Long.class.equals(f.getType())) {
                        f.set(object, Long.parseLong(value));
                    } else if (Double.TYPE.equals(f.getType()) || Double.class.equals(f.getType())) {
                        f.set(object, Double.parseDouble(value));
                    } else if (Enum.class.isAssignableFrom(f.getType())) {
                        f.set(object, create(f.getType(), value));
                    } else if (Map.class.equals(f.getType())) {

                        System.out.println("!!!!");
                        System.out.println(value);
                        //f.set(object, toMap(value));
                    } else {
                        //System.out.println(object.getClass().getName() + " " + f.getName() + " " + f.getType().getName());
                    }
                } catch (final IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Enum<?> create(final Class<?> type, final String value) {
        return Enum.valueOf((Class<Enum>) type, value);
    }

    // takes proper case variable name 'fooBar' and changes it to 'FOO_BAR'
    //
    // configuration classes should not have properties like 'typeJSON' as that would turn
    // into environment variable 'TYPE_J_S_O_N'. This is fine since these configuration
    // classes are unique to this project (just like this utility method) - this is not
    // a generic library to be used externally by others not following this limitation.
    //
    private static String getEnvironmentVariable(final String string, final String prefix) {
        return prefix + PATTERN.matcher(string).replaceAll(match -> "_" + match.group()).toUpperCase();
    }

    private static String getPropertyKey(final String string, final String prefix) {
        return prefix.replace("_", ".").toLowerCase() + PATTERN.matcher(string).replaceAll(match -> "." + match.group()).toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, Object> getMap(T object, Field f) {
        try {
            Map<String, Object> map = (Map<String, Object>) f.get(object);
            if (map == null) {
                map = new HashMap<>();
                f.set(object, map);
            }
            return map;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String extractName(final String name) {
        final String prefix = "set";
        if (name.startsWith(prefix) && name.length() > prefix.length()) {
            return Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
        } else {
            return name;
        }
    }

    private static boolean toBoolean(String string) {
        if (string == null) {
            return false;
        }
        return ("true".equalsIgnoreCase(string) || "t".equalsIgnoreCase(string));
    }

}
