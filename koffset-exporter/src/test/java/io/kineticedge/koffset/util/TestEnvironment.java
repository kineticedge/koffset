package io.kineticedge.koffset.util;

import java.util.HashMap;
import java.util.Map;

public class TestEnvironment extends Environment {

    Map<String, String> env = new HashMap<>();

    @Override
    public Map<String, String> getAll() {
        return env;
    }

    @Override
    public String get(String key) {
        return env.get(key);
    }

    // testing 'hooks'

    public void put(String key, String value) {
        env.put(key, value);
    }

    public void clear() {
        env.clear();
    }

}
