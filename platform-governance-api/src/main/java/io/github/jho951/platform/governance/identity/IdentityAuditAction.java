package io.github.jho951.platform.governance.identity;

import io.github.jho951.platform.governance.api.AuditAction;
import io.github.jho951.platform.governance.api.AuditCategory;
import io.github.jho951.platform.governance.api.AuditResult;

public enum IdentityAuditAction implements AuditAction {
    IDENTITY_LOGIN_SUCCEEDED(AuditResult.SUCCESS),
    IDENTITY_LOGIN_FAILED(AuditResult.FAILURE),
    IDENTITY_LOGOUT(AuditResult.SUCCESS),
    IDENTITY_TOKEN_ISSUED(AuditResult.SUCCESS),
    IDENTITY_TOKEN_REFRESHED(AuditResult.SUCCESS),
    IDENTITY_TOKEN_REVOKED(AuditResult.SUCCESS),
    IDENTITY_TOKEN_REUSE_DETECTED(AuditResult.FAILURE),
    IDENTITY_SESSION_CREATED(AuditResult.SUCCESS),
    IDENTITY_SESSION_FORCE_LOGOUT(AuditResult.SUCCESS),
    IDENTITY_CREDENTIAL_RESET(AuditResult.SUCCESS),
    IDENTITY_ADMIN_SESSION_TERMINATED(AuditResult.SUCCESS),
    IDENTITY_SIGNING_KEY_ROTATED(AuditResult.SUCCESS),
    IDENTITY_INTERNAL_CALLER_DENIED(AuditResult.DENIED),
    IDENTITY_ADMIN_IP_DENIED(AuditResult.DENIED);

    private final AuditResult defaultResult;

    IdentityAuditAction(AuditResult defaultResult) {
        this.defaultResult = defaultResult;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public AuditCategory category() {
        return AuditCategory.IDENTITY;
    }

    public AuditResult defaultResult() {
        return defaultResult;
    }
}
