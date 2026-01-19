package io.kineticedge.koffset.util;

import io.kineticedge.koffset.config.KoffsetConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EnvConfigLoader extends ConfigLoader {

    private final Environment env;

    public EnvConfigLoader() {
        this(new Environment());
    }

    public EnvConfigLoader(Environment env) {
        this.env = env;
    }

    //

    // properCase to SNAKE_CASE
    @Override
    protected String getKey(String name, String prefix) {
        String snake = name.replaceAll("(?<=[a-z])[A-Z]", "_$0").toUpperCase();
        return prefix + snake;
    }

    @Override
    protected Optional<String> getValue(String key) {
        return env.get(key);
    }

    @Override
    protected java.util.Set<String> getAllKeys() {
        return env.getAll().keySet();
    }

    @Override
    protected char delimiter() {
        return '_';
    }

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

    public static void main(String[] args) {
        TestEnvironment e = new TestEnvironment();
        e.put("KOFFSET_KAFKA_BOOTSTRAP_SERVERS", "a");
        e.put("KOFFSET_KAFKA_FOO__BAR", "a");
        e.put("KOFFSET_COLLECTOR_ADMIN_TIMEOUT", "555");
        e.put("KOFFSET_COLLECTOR", "555");
        EnvConfigLoader config = new EnvConfigLoader(e);
        KoffsetConfig koffsetConfig = new KoffsetConfig();
        config.populate(koffsetConfig, "KOFFSET_");
        System.out.println(koffsetConfig);
    }
}