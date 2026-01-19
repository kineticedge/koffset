package io.kineticedge.koffset.util;

import io.kineticedge.koffset.config.KoffsetConfig;

import java.util.Optional;
import java.util.Properties;

public class PropertyConfigLoader extends ConfigLoader {

    private final Properties properties;

    public PropertyConfigLoader(Properties properties) {
        this.properties = properties;
    }

    //

    @Override
    protected String getKey(String name, String prefix) {
        String snake = name.replaceAll("(?<=[a-z])[A-Z]", ".$0").toLowerCase();
        return prefix + snake;
    }

    @Override
    protected Optional<String> getValue(String key) {
        return properties.getProperty(key) != null ? Optional.of(properties.getProperty(key)) : Optional.empty();
    }

    @Override
    protected java.util.Set<String> getAllKeys() {
        return properties.stringPropertyNames();
    }

    @Override
    protected char delimiter() {
        return '.';
    }

    public static void main(String[] args) {

        Properties p = new Properties();
        p.put("koffset.kafka.bootstrap.servers", "a");
        p.put("koffset.kafka.foo..bar", "a");
        p.put("koffset.collector.admin.timeout", "555");

        ConfigLoader config = new PropertyConfigLoader(p);
        KoffsetConfig koffsetConfig = new KoffsetConfig();
        config.populate(koffsetConfig, "koffset");
        System.out.println(koffsetConfig);
    }
}