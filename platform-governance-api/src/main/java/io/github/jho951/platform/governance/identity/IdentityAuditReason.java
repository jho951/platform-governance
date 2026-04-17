package io.github.jho951.platform.governance.identity;

import io.github.jho951.platform.governance.api.AuditReason;

public enum IdentityAuditReason implements AuditReason {
    INVALID_CREDENTIAL,
    IP_DENIED,
    REVOKED,
    TOKEN_REUSE_DETECTED,
    EXPIRED,
    USER_REQUEST,
    ADMIN_REQUEST,
    POLICY_DENIED,
    INTERNAL_CALLER_DENIED,
    KEY_ROTATION,
    UNKNOWN;

    @Override
    public String code() {
        return name();
    }
}
