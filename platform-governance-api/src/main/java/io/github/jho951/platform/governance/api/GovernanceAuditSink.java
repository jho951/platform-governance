package io.github.jho951.platform.governance.api;

/**
 * Platform-owned audit delivery SPI for governance events.
 *
 * <p>Mainline governance consumers should register this sink instead of
 * directly exposing raw audit-log library types on the service compile
 * surface.</p>
 */
@FunctionalInterface
public interface GovernanceAuditSink {
    void write(AuditEntry entry);
}
