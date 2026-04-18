package io.github.jho951.platform.governance.config;

import io.github.jho951.platform.governance.api.PolicyConfigSource;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public final class MapPolicyConfigSource implements PolicyConfigSource {
    private final Map<String, String> values;

    public MapPolicyConfigSource(Map<String, String> values) {
        this.values = values == null ? Collections.emptyMap() : Map.copyOf(values);
    }

    @Override
    public Optional<String> resolve(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public Map<String, String> snapshot() {
        return values;
    }

    @Override
    public boolean isOperational() {
        return !values.isEmpty();
    }
}
