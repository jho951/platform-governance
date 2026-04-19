package io.github.jho951.platform.governance.config;

import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
import io.github.jho951.platform.governance.api.PolicyConfigSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CompositePolicyConfigSource implements PolicyConfigSource {
    private final List<PolicyConfigSource> sources;

    public CompositePolicyConfigSource(List<PolicyConfigSource> sources) {
        this.sources = sources == null ? List.of() : sources.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<String> resolve(String key) {
        for (PolicyConfigSource source : sources) {
            Optional<String> value = source.resolve(key);
            if (value.isPresent()) return value;
        }
        return Optional.empty();
    }

    @Override
    public Map<String, String> snapshot() {
        if (!supportsSnapshot()) {
            throw new IllegalStateException("All policy config sources must support snapshots");
        }
        return sources.stream()
                .map(PolicyConfigSource::snapshot)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    @Override
    public boolean supportsSnapshot() {
        return sources.stream().allMatch(PolicyConfigSource::supportsSnapshot);
    }

    @Override
    @Deprecated(since = "2.0.1", forRemoval = true)
    @SuppressWarnings("removal")
    public boolean isOperational() {
        return operationalStatus().isOperational();
    }

    @Override
    public PolicyConfigOperationalStatus operationalStatus() {
        if (sources.isEmpty()) {
            return PolicyConfigOperationalStatus.notConfigured("no policy config sources configured");
        }
        for (PolicyConfigSource source : sources) {
            PolicyConfigOperationalStatus status = source.operationalStatus();
            if (!status.isOperational()) {
                return status;
            }
        }
        return PolicyConfigOperationalStatus.operational("all policy config sources are operational");
    }
}
