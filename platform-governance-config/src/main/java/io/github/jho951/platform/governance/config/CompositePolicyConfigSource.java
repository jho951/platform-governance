package io.github.jho951.platform.governance.config;

import io.github.jho951.platform.governance.api.PolicyConfigSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CompositePolicyConfigSource implements PolicyConfigSource {
    private final List<PolicyConfigSource> sources;

    public CompositePolicyConfigSource(List<PolicyConfigSource> sources) {
        this.sources = sources == null ? List.of() : List.copyOf(sources);
    }

    @Override
    public Optional<String> resolve(String key) {
        for (PolicyConfigSource source : sources) {
            Optional<String> value = source.resolve(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> snapshot() {
        return sources.stream()
                .map(PolicyConfigSource::snapshot)
                .flatMap(map -> map.entrySet().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }
}
