package io.kineticedge.koffset.util;

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

    // SNAKE_CASE
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

}