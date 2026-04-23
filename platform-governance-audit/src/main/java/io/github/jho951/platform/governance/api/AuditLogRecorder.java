package io.github.jho951.platform.governance.api;

/**
 * Internal audit-recording adapter used by platform-governance internals and
 * platform-integrations bridges.
 *
 * <p>Mainline governance consumers should register {@code AuditSink} beans
 * instead of treating this adapter as a public extension point.</p>
 */
public interface AuditLogRecorder {
    void record(AuditEntry entry);
}
