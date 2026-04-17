package io.github.jho951.platform.governance.identity;

import io.github.jho951.platform.governance.api.AuditActor;

public interface IdentityAuditRecorder {
    void record(IdentityAuditEvent event);

    default void loginSucceeded(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_LOGIN_SUCCEEDED));
    }

    default void loginSucceeded(String userId) {
        loginSucceeded(IdentityAuditEvent.loginSucceeded()
                .actor(AuditActor.user(userId))
                .build());
    }

    default void loginFailed(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_LOGIN_FAILED));
    }

    default void loginFailed(String loginId, IdentityAuditReason reason) {
        loginFailed(IdentityAuditEvent.loginFailed()
                .actor(AuditActor.anonymous(loginId))
                .loginId(loginId)
                .reason(reason)
                .build());
    }

    default void logout(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_LOGOUT));
    }

    default void tokenIssued(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_TOKEN_ISSUED));
    }

    default void tokenRefreshed(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_TOKEN_REFRESHED));
    }

    default void tokenRevoked(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_TOKEN_REVOKED));
    }

    default void tokenReuseDetected(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_TOKEN_REUSE_DETECTED));
    }

    default void sessionCreated(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_SESSION_CREATED));
    }

    default void sessionForceLoggedOut(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_SESSION_FORCE_LOGOUT));
    }

    default void credentialReset(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_CREDENTIAL_RESET));
    }

    default void signingKeyRotated(IdentityAuditEvent event) {
        record(event.withAction(IdentityAuditAction.IDENTITY_SIGNING_KEY_ROTATED));
    }
}
