package io.github.jho951.platform.governance.identity;

public interface IdentityAuditCustomizer {
    IdentityAuditEvent customize(IdentityAuditEvent event);
}
