package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditResult;
import com.auditlog.api.AuditSink;
import io.github.jho951.platform.governance.api.AuditEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogGovernanceAuditSinkTest {

    @Test
    void translatesGovernanceEntryIntoAuditLogEvent() {
        CapturingAuditSink delegate = new CapturingAuditSink();
        AuditLogGovernanceAuditSink sink = new AuditLogGovernanceAuditSink(delegate);
        Instant occurredAt = Instant.parse("2026-04-27T01:02:03Z");

        sink.write(new AuditEntry(
                "auth",
                "AUTH_LOGIN_PASSWORD",
                Map.ofEntries(
                        Map.entry("eventType", "LOGIN"),
                        Map.entry("result", "SUCCESS"),
                        Map.entry("actorType", "USER"),
                        Map.entry("actorId", "user-1"),
                        Map.entry("resourceType", "AUTH_ACCOUNT"),
                        Map.entry("resourceId", "user-1"),
                        Map.entry("traceId", "trace-1"),
                        Map.entry("requestId", "request-1"),
                        Map.entry("clientIp", "127.0.0.1"),
                        Map.entry("userAgent", "JUnit"),
                        Map.entry("reason", "LOGIN_OK")
                ),
                occurredAt
        ));

        AuditEvent event = delegate.event;
        assertEquals(AuditEventType.LOGIN, event.getEventType());
        assertEquals("AUTH_LOGIN_PASSWORD", event.getAction());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals("user-1", event.getActorId());
        assertEquals(AuditActorType.USER, event.getActorType());
        assertEquals("AUTH_ACCOUNT", event.getResourceType());
        assertEquals("user-1", event.getResourceId());
        assertEquals(AuditResult.SUCCESS, event.getResult());
        assertEquals("LOGIN_OK", event.getReason());
        assertEquals("trace-1", event.getTraceId());
        assertEquals("request-1", event.getRequestId());
        assertEquals("127.0.0.1", event.getClientIp());
        assertEquals("JUnit", event.getUserAgent());
        assertEquals("auth", event.getDetails().get("category"));
    }

    @Test
    void writesDurableAuditLineToFile(@TempDir Path tempDir) throws Exception {
        Path auditFile = tempDir.resolve("governance-audit.log");
        AuditLogGovernanceAuditSink sink = AuditLogGovernanceAuditSink.durable(
                auditFile,
                "auth-service",
                "prod",
                false,
                2,
                1000,
                null,
                null
        );

        sink.write(new AuditEntry(
                "auth",
                "AUTH_LOGIN_PASSWORD",
                Map.of(
                        "eventType", "LOGIN",
                        "result", "SUCCESS",
                        "actorType", "USER",
                        "actorId", "user-1",
                        "resourceType", "AUTH_ACCOUNT",
                        "resourceId", "user-1"
                ),
                Instant.parse("2026-04-27T01:02:03Z")
        ));
        sink.close();

        String content = Files.readString(auditFile);
        assertTrue(content.contains("AUTH_LOGIN_PASSWORD"));
        assertTrue(content.contains("user-1"));
        assertTrue(content.contains("AUTH_ACCOUNT"));
    }

    private static final class CapturingAuditSink implements AuditSink {
        private AuditEvent event;

        @Override
        public void write(AuditEvent event) {
            this.event = event;
        }
    }
}
