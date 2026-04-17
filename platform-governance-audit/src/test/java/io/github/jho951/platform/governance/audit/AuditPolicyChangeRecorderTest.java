package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.PolicyChangeEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditPolicyChangeRecorderTest {
    @Test
    void redactsSensitivePolicyValues() {
        List<AuditEntry> entries = new ArrayList<>();
        AuditPolicyChangeRecorder recorder = new AuditPolicyChangeRecorder(entries::add);

        recorder.record(new PolicyChangeEvent(
                "operator-1",
                "security.jwt-secret",
                "old-secret",
                "new-secret",
                "rotate secret",
                Map.of("accessToken", "raw-token"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        Map<String, String> attributes = entries.get(0).attributes();
        assertEquals("[REDACTED]", attributes.get("policy.previous-value"));
        assertEquals("[REDACTED]", attributes.get("policy.new-value"));
        assertEquals("[REDACTED]", attributes.get("accessToken"));
    }
}
