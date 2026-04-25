package io.github.jho951.platform.governance.api;

/**
 * Public governance audit recording contract for services and 2nd-layer
 * bridges that need to publish structured audit entries.
 */
@FunctionalInterface
public interface GovernanceAuditRecorder {
    void record(AuditEntry entry);
}
