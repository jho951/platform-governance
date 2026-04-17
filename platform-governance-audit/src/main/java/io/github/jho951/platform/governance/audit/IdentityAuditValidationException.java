package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.identity.IdentityAuditAction;

import java.util.List;

public final class IdentityAuditValidationException extends RuntimeException {
    private final IdentityAuditAction action;
    private final List<String> errors;

    public IdentityAuditValidationException(IdentityAuditAction action, List<String> errors) {
        super("Invalid identity audit event " + action + ": " + String.join("; ", errors));
        this.action = action;
        this.errors = List.copyOf(errors);
    }

    public IdentityAuditAction action() {
        return action;
    }

    public List<String> errors() {
        return errors;
    }
}
