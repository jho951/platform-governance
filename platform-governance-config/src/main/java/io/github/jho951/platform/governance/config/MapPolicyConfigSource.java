package io.github.jho951.platform.governance.config;

import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
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
    @Deprecated(since = "2.0.1", forRemoval = true)
    @SuppressWarnings("removal")
    public boolean isOperational() {
        return operationalStatus().isOperational();
    }

    @Override
    public PolicyConfigOperationalStatus operationalStatus() {
        if (values.isEmpty()) {
            return PolicyConfigOperationalStatus.notConfigured("map policy config has no values");
        }
        return PolicyConfigOperationalStatus.operational("map policy config has " + values.size() + " value(s)");
    }
}
