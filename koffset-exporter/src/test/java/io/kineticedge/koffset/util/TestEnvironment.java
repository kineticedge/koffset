package io.kineticedge.koffset.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestEnvironment extends Environment {

    Map<String, String> env = new HashMap<>();

    @Override
    public Map<String, String> getAll() {
        return env;
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.of(env.get(key));
    }

    // testing 'hooks'

    public void put(String key, String value) {
        env.put(key, value);
    }

    public void clear() {
        env.clear();
    }

}
