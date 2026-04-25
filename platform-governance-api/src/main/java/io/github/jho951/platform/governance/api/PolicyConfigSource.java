package io.github.jho951.platform.governance.api;

import java.util.Map;
import java.util.Optional;

public interface PolicyConfigSource {
    Optional<String> resolve(String key);

    Map<String, String> snapshot();

    default boolean supportsSnapshot() {
        return true;
    }

    /**
     * Returns the current operational readiness state for enforcing policy decisions.
     */
    default PolicyConfigOperationalStatus operationalStatus() {
        return PolicyConfigOperationalStatus.notConfigured("PolicyConfigSource did not declare an operational status");
    }
}
