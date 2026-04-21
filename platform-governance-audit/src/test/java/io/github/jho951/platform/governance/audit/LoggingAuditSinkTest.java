package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingAuditSinkTest {

    @Test
    void writesStructuredAuditLineToLogger() {
        Logger logger = Logger.getLogger("test.logging-audit-sink");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);
        try {
            LoggingAuditSink sink = new LoggingAuditSink(logger, Level.INFO);

            sink.write(AuditEvent.builder(AuditEventType.SYSTEM, "policy evaluated")
                    .eventId("event-1")
                    .occurredAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .actor("platform-governance", AuditActorType.SYSTEM, "platform-governance")
                    .resource("governance", "governance")
                    .result(AuditResult.SUCCESS)
                    .detail("audit.service-name", "auth-service")
                    .build());

            assertTrue(handler.messages.get(0).contains("eventId=event-1"));
            assertTrue(handler.messages.get(0).contains("action=policy evaluated"));
            assertTrue(handler.messages.get(0).contains("details={audit.service-name=auth-service}"));
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static final class CapturingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
