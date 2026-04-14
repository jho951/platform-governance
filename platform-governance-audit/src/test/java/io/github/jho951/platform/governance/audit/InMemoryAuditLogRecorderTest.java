package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryAuditLogRecorderTest {
    @Test
    void recordsEntries() {
        InMemoryAuditLogRecorder recorder = new InMemoryAuditLogRecorder();
        recorder.record(new AuditEntry("governance", "accepted", Map.of("k", "v"), Instant.parse("2026-01-01T00:00:00Z")));

        assertEquals(1, recorder.entries().size());
    }
}
