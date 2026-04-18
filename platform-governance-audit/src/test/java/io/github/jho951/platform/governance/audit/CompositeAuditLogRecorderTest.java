package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeAuditLogRecorderTest {
    @Test
    void fansOutAuditEntriesToAllRecorders() {
        List<String> categories = new ArrayList<>();
        CompositeAuditLogRecorder recorder = new CompositeAuditLogRecorder(List.of(
                entry -> categories.add("first:" + entry.category()),
                entry -> categories.add("second:" + entry.category())
        ));

        recorder.record(new AuditEntry(
                "governance",
                "policy evaluated",
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        assertEquals(List.of("first:governance", "second:governance"), categories);
    }
}
