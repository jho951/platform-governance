package io.github.jho951.platform.governance.api;

public enum GovernanceEngineFailurePolicy {
    FAIL_CLOSED,
    FAIL_OPEN,

    /**
     * @deprecated since 2.0.1. Use {@link #FAIL_CLOSED}; this compatibility
     * alias has the same runtime behavior and will be removed in 3.1.0.
     */
    @Deprecated(since = "2.0.1", forRemoval = true)
    AUDIT_AND_DENY
}
