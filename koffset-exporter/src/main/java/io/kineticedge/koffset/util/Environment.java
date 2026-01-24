
package io.kineticedge.koffset.util;

import java.util.Map;
import java.util.Optional;

import static io.kineticedge.koffset.util.StringUtil.isBlank;

/**
 * A wrapper class to Environment to make tests easier.
 */
public class Environment {

    // all public methods should use these 2 methods, then MockEnvironment replaces these with alternate for testing.

    protected Map<String, String> getEnv() {
        return System.getenv();
    }

    protected String getEnv(String name) {
        return System.getenv(name);
    }

    // public methods

    public Map<String, String> getAll() {
        return getEnv();
    }

    //TODO
    public Map<String, String> getAll(String prefix) {
        //return getEnv();
        //return getEnv().keySet().stream().filter(e -> e.startsWith(prefix)).toList();
        return null;
    }

    public String get(String name, String defaultValue) {
        final String val = getEnv(name);
        if (isBlank(val)) {
            return defaultValue;
        }
        return val;
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(getEnv(name));
    }

    public long getAsLong(String name, long defaultValue) {

        final String val = getEnv(name);

        if (isBlank(val)) {
            return defaultValue;
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
