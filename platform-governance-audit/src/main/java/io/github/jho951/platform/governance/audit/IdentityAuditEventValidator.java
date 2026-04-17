package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditActor;
import io.github.jho951.platform.governance.api.AuditTarget;
import io.github.jho951.platform.governance.identity.IdentityAuditAction;
import io.github.jho951.platform.governance.identity.IdentityAuditEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IdentityAuditEventValidator {
    public List<String> validate(IdentityAuditEvent event) {
        Objects.requireNonNull(event, "event");
        List<String> errors = new ArrayList<>();
        require(event.action() != null, errors, "action is required");
        require(event.actor() != null && event.actor().type() != AuditActor.Type.UNKNOWN, errors, "actor.type is required");
        require(event.result() != null, errors, "result is required");

        if (event.action() == IdentityAuditAction.IDENTITY_LOGIN_FAILED) {
            require(event.actor().hasId() || hasText(event.loginId()), errors, "actor.id or loginId is required for login failure");
            require(event.reason() != null, errors, "reason is required for login failure");
            require(hasText(event.correlation().clientIp()), errors, "clientIp is required for login failure");
        }
        if (event.action() == IdentityAuditAction.IDENTITY_TOKEN_REVOKED) {
            require(event.actor().hasId(), errors, "actor.id is required for token revoked");
            require(event.target().type() == AuditTarget.Type.TOKEN && event.target().hasId(), errors, "target token id is required for token revoked");
            require(event.reason() != null, errors, "reason is required for token revoked");
        }
        if (event.action() == IdentityAuditAction.IDENTITY_TOKEN_REUSE_DETECTED) {
            require(event.target().type() == AuditTarget.Type.TOKEN && event.target().hasId(), errors, "target token id is required for token reuse detection");
            require(event.reason() != null, errors, "reason is required for token reuse detection");
        }
        if (event.action() == IdentityAuditAction.IDENTITY_SESSION_FORCE_LOGOUT
                || event.action() == IdentityAuditAction.IDENTITY_ADMIN_SESSION_TERMINATED) {
            require(event.actor().hasId(), errors, "admin actor id is required for session termination");
            require(event.target().hasId(), errors, "target user id or session id is required for session termination");
            require(event.reason() != null, errors, "reason is required for session termination");
        }
        if (event.action() == IdentityAuditAction.IDENTITY_SIGNING_KEY_ROTATED) {
            require(event.actor().hasId(), errors, "actor.id is required for signing key rotation");
            require(event.target().type() == AuditTarget.Type.SIGNING_KEY && event.target().hasId(), errors, "target key id is required for signing key rotation");
            require(hasText(event.attributes().get("oldKeyId")), errors, "oldKeyId is required for signing key rotation");
            require(hasText(event.attributes().get("newKeyId")), errors, "newKeyId is required for signing key rotation");
            require(event.reason() != null, errors, "reason is required for signing key rotation");
        }
        return List.copyOf(errors);
    }

    private static void require(boolean condition, List<String> errors, String message) {
        if (!condition) {
            errors.add(message);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
