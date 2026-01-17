package io.kineticedge.koffset.util;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KafkaEnv {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaEnv.class);

    private static final Pattern STARTS_WITH_NUMBER = Pattern.compile("^[0-9].*$");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    private final Environment environment;

    public KafkaEnv() {
        this.environment = new Environment();
    }

    // use for testing
    protected KafkaEnv(Environment environment) {
        this.environment = environment;
    }

    //
    // If prefix is "KAFKA_" then the following is true
    //
    // KAFKA_BOOTSTRAP_SERVERS -> "bootstrap.servers"
    // KAFKA_sEcUrItY_PrOtOcOL -> "security.protocol"
    //
    // The prefix must be an exact match (case and any delimiter included in the prefix), but what follows will be
    // accepted, converted to lowercase, and '_'changed to '.'.
    //
    public Map<String, Object> to(final String prefix) {
        return environment.getAll().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> {
                    final String key = e.getKey().substring(prefix.length()).replaceAll("(?<!_)_(?!_)", ".").replaceAll("__", "_").toLowerCase();
                    return Map.entry(key, e.getValue());
                })
                .filter(e -> !e.getKey().isEmpty() && !STARTS_WITH_NUMBER.matcher(e.getKey()).matches())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2));
    }

    //TODO
    private String resolvePlaceholders(String value) {
        if (value == null || !value.contains("{{")) {
            return value;
        }

        StringBuilder sb = new StringBuilder();
        var matcher = PLACEHOLDER_PATTERN.matcher(value);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(value, lastEnd, matcher.start());
            String envVarName = matcher.group(1);
            String envValue = environment.getEnv(envVarName); // Look up in global env

            if (envValue != null) {
                sb.append(envValue);
            } else {
                log.warn("Placeholder {{{}}}} found but no matching environment variable exists.", envVarName);
                sb.append(matcher.group(0)); // Keep original {{VAR}}
            }
            lastEnd = matcher.end();
        }
        sb.append(value.substring(lastEnd));

        return sb.toString();
    }
}
