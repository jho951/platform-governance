package io.github.jho951.platform.governance.api;

public interface AuditReason {
    String code();

    static AuditReason of(String code) {
        String normalized = code == null || code.isBlank() ? "UNKNOWN" : code.trim();
        return () -> normalized;
    }
}
