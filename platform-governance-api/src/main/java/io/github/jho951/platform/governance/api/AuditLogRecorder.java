package io.github.jho951.platform.governance.api;

/**
 * Internal audit-recording adapter used by governance components.
 *
 * <p>For production delivery, Spring consumers should register audit-log
 * {@code AuditSink} beans. This adapter exists to bridge platform governance
 * events into the audit pipeline and to support tests or specialized
 * composition. Spring auto-configuration ignores external implementations by
 * default and only re-enables fan-out through the temporary compat flag.</p>
 */
public interface AuditLogRecorder {
    void record(AuditEntry entry);
}
