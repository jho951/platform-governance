package io.github.jho951.platform.governance.api;

public enum AuditFailurePolicy {
    FAIL_CLOSED,
    FAIL_OPEN_WITH_ALERT,
    RETRY_THEN_FAIL,
    IGNORE
}
