package io.github.jho951.platform.governance.identity;

import io.github.jho951.platform.governance.api.AuditActor;
import io.github.jho951.platform.governance.api.AuditCorrelation;
import io.github.jho951.platform.governance.api.AuditReason;
import io.github.jho951.platform.governance.api.AuditResult;
import io.github.jho951.platform.governance.api.AuditSeverity;
import io.github.jho951.platform.governance.api.AuditTarget;
import io.github.jho951.platform.governance.api.PolicyEvidence;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record IdentityAuditEvent(
        IdentityAuditAction action,
        AuditActor actor,
        AuditTarget target,
        AuditResult result,
        AuditReason reason,
        AuditSeverity severity,
        AuditCorrelation correlation,
        PolicyEvidence policyEvidence,
        String channel,
        String provider,
        String loginId,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public IdentityAuditEvent {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        actor = actor == null ? AuditActor.unknown() : actor;
        target = target == null ? AuditTarget.unknown() : target;
        result = result == null ? action.defaultResult() : result;
        severity = severity == null ? defaultSeverity(action, result) : severity;
        correlation = correlation == null ? AuditCorrelation.EMPTY : correlation;
        policyEvidence = policyEvidence == null ? PolicyEvidence.empty() : policyEvidence;
        channel = normalize(channel);
        provider = normalize(provider);
        loginId = normalize(loginId);
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static Builder builder(IdentityAuditAction action) {
        return new Builder(action);
    }

    public static Builder loginSucceeded() {
        return builder(IdentityAuditAction.IDENTITY_LOGIN_SUCCEEDED);
    }

    public static Builder loginFailed() {
        return builder(IdentityAuditAction.IDENTITY_LOGIN_FAILED);
    }

    public static Builder logout() {
        return builder(IdentityAuditAction.IDENTITY_LOGOUT);
    }

    public static Builder tokenIssued() {
        return builder(IdentityAuditAction.IDENTITY_TOKEN_ISSUED);
    }

    public static Builder tokenRefreshed() {
        return builder(IdentityAuditAction.IDENTITY_TOKEN_REFRESHED);
    }

    public static Builder tokenRevoked() {
        return builder(IdentityAuditAction.IDENTITY_TOKEN_REVOKED);
    }

    public static Builder tokenReuseDetected() {
        return builder(IdentityAuditAction.IDENTITY_TOKEN_REUSE_DETECTED);
    }

    public static Builder sessionCreated() {
        return builder(IdentityAuditAction.IDENTITY_SESSION_CREATED);
    }

    public static Builder sessionForceLoggedOut() {
        return builder(IdentityAuditAction.IDENTITY_SESSION_FORCE_LOGOUT);
    }

    public static Builder credentialReset() {
        return builder(IdentityAuditAction.IDENTITY_CREDENTIAL_RESET);
    }

    public static Builder signingKeyRotated() {
        return builder(IdentityAuditAction.IDENTITY_SIGNING_KEY_ROTATED);
    }

    public IdentityAuditEvent withAction(IdentityAuditAction action) {
        return toBuilder().action(action).build();
    }

    public Builder toBuilder() {
        return builder(action)
                .actor(actor)
                .target(target)
                .result(result)
                .reason(reason)
                .severity(severity)
                .correlation(correlation)
                .policyEvidence(policyEvidence)
                .channel(channel)
                .provider(provider)
                .loginId(loginId)
                .attributes(attributes)
                .occurredAt(occurredAt);
    }

    private static AuditSeverity defaultSeverity(IdentityAuditAction action, AuditResult result) {
        if (action == IdentityAuditAction.IDENTITY_TOKEN_REUSE_DETECTED
                || action == IdentityAuditAction.IDENTITY_INTERNAL_CALLER_DENIED
                || action == IdentityAuditAction.IDENTITY_ADMIN_IP_DENIED) {
            return AuditSeverity.CRITICAL;
        }
        if (result == AuditResult.FAILURE || result == AuditResult.DENIED) {
            return AuditSeverity.WARN;
        }
        return AuditSeverity.INFO;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class Builder {
        private IdentityAuditAction action;
        private AuditActor actor;
        private AuditTarget target;
        private AuditResult result;
        private AuditReason reason;
        private AuditSeverity severity;
        private AuditCorrelation correlation;
        private PolicyEvidence policyEvidence;
        private String channel;
        private String provider;
        private String loginId;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private Instant occurredAt;

        private Builder(IdentityAuditAction action) {
            this.action = action;
        }

        public Builder action(IdentityAuditAction action) {
            this.action = action;
            return this;
        }

        public Builder actor(AuditActor actor) {
            this.actor = actor;
            return this;
        }

        public Builder target(AuditTarget target) {
            this.target = target;
            return this;
        }

        public Builder result(AuditResult result) {
            this.result = result;
            return this;
        }

        public Builder reason(AuditReason reason) {
            this.reason = reason;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = AuditReason.of(reason);
            return this;
        }

        public Builder severity(AuditSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder correlation(AuditCorrelation correlation) {
            this.correlation = correlation;
            return this;
        }

        public Builder policyEvidence(PolicyEvidence policyEvidence) {
            this.policyEvidence = policyEvidence;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder loginId(String loginId) {
            this.loginId = loginId;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (key != null && !key.isBlank() && value != null) {
                attributes.put(key.trim(), value);
            }
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            if (attributes != null) {
                attributes.forEach(this::attribute);
            }
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public IdentityAuditEvent build() {
            return new IdentityAuditEvent(
                    action,
                    actor,
                    target,
                    result,
                    reason,
                    severity,
                    correlation,
                    policyEvidence,
                    channel,
                    provider,
                    loginId,
                    attributes,
                    occurredAt
            );
        }
    }
}
