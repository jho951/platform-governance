package io.github.jho951.platform.governance.config;

import com.policyconfig.contracts.PolicyKey;
import com.policyconfig.contracts.PolicyResolution;
import com.policyconfig.contracts.PolicyResolver;
import com.policyconfig.contracts.PolicySnapshotProvider;
import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
import io.github.jho951.platform.governance.api.PolicyConfigSource;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PolicyResolverPolicyConfigSource implements PolicyConfigSource {
    private final PolicyResolver policyResolver;

    public PolicyResolverPolicyConfigSource(PolicyResolver policyResolver) {
        this.policyResolver = Objects.requireNonNull(policyResolver, "policyResolver");
    }

    @Override
    public Optional<String> resolve(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        PolicyResolution<String> resolution = policyResolver.inspect(PolicyKey.builder(key, String.class).build());
        return resolution.present() ? Optional.ofNullable(resolution.value()) : Optional.empty();
    }

    @Override
    public Map<String, String> snapshot() {
        if (policyResolver instanceof PolicySnapshotProvider snapshotProvider) {
            return snapshotProvider.snapshotValues();
        }
        throw new IllegalStateException("PolicyResolver does not support policy config snapshot");
    }

    @Override
    public boolean supportsSnapshot() {
        return policyResolver instanceof PolicySnapshotProvider;
    }

    @Override
    @Deprecated(since = "2.0.1", forRemoval = true)
    @SuppressWarnings("removal")
    public boolean isOperational() {
        return operationalStatus().isOperational();
    }

    @Override
    public PolicyConfigOperationalStatus operationalStatus() {
        return PolicyConfigOperationalStatus.operational("PolicyResolver is available");
    }
}
