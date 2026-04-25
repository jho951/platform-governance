package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizingGovernanceAuditRecorderTest {
    @Test
    void enrichesAndRedactsGenericAuditEntries() {
        List<AuditEntry> entries = new ArrayList<>();
        SanitizingGovernanceAuditRecorder recorder = new SanitizingGovernanceAuditRecorder(
                entries::add,
                Map.of("audit.service-name", "auth-service", "audit.environment", "prod")
        );

        recorder.record(new AuditEntry(
                "governance",
                "policy evaluated",
                Map.of("clientSecret", "secret", "request.action", "read"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        Map<String, String> attributes = entries.get(0).attributes();
        assertEquals("auth-service", attributes.get("audit.service-name"));
        assertEquals("prod", attributes.get("audit.environment"));
        assertEquals("read", attributes.get("request.action"));
        assertEquals("[REDACTED]", attributes.get("clientSecret"));
    }
}
