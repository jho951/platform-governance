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
     * Returns whether this source is safe to use for enforcing policy decisions.
     *
     * @deprecated since 2.0.1. Use {@link #operationalStatus()} so callers can
     * distinguish missing configuration, unavailable backing stores, and unknown
     * states. This method will be removed in 3.0.0.
     */
    @Deprecated(since = "2.0.1", forRemoval = true)
    default boolean isOperational() {
        return false;
    }

    /**
     * Returns the current operational readiness state for enforcing policy decisions.
     */
    default PolicyConfigOperationalStatus operationalStatus() {
        if (isOperational()) {
            return PolicyConfigOperationalStatus.operational("legacy isOperational=true");
        }
        return PolicyConfigOperationalStatus.notConfigured("legacy isOperational=false");
    }
}
