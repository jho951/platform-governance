package io.github.jho951.platform.governance.api;

public interface AuditLogRecorder {
    void record(AuditEntry entry);
}

