package io.github.jho951.platform.governance.api;

/**
 * Describes the response that runs after a violation is detected.
 * The final allow/deny result is decided by {@link GovernanceVerdict}.
 */
public enum ViolationAction {
    AUDIT_ONLY,
    DENY,
    ALERT,
    ESCALATE
}
