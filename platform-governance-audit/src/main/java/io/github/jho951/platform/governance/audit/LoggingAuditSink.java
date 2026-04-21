package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditSink;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes audit events to the application log so local and test environments do
 * not silently drop governance audits when no external sink is configured.
 */
public final class LoggingAuditSink implements AuditSink {
    private final Logger logger;
    private final Level level;

    public LoggingAuditSink() {
        this(Logger.getLogger(LoggingAuditSink.class.getName()), Level.INFO);
    }

    LoggingAuditSink(Logger logger, Level level) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.level = Objects.requireNonNull(level, "level");
    }

    @Override
    public void write(AuditEvent event) {
        logger.log(level, () -> format(event));
    }

    private static String format(AuditEvent event) {
        StringJoiner joiner = new StringJoiner(", ", "audit{", "}");
        append(joiner, "eventId", event.getEventId());
        append(joiner, "occurredAt", event.getOccurredAt());
        append(joiner, "actorId", event.getActorId());
        append(joiner, "actorType", event.getActorType());
        append(joiner, "actorName", event.getActorName());
        append(joiner, "eventType", event.getEventType());
        append(joiner, "action", event.getAction());
        append(joiner, "resourceType", event.getResourceType());
        append(joiner, "resourceId", event.getResourceId());
        append(joiner, "result", event.getResult());
        append(joiner, "reason", event.getReason());
        append(joiner, "traceId", event.getTraceId());
        append(joiner, "requestId", event.getRequestId());
        append(joiner, "clientIp", event.getClientIp());
        append(joiner, "userAgent", event.getUserAgent());
        append(joiner, "details", formatDetails(event.getDetails()));
        return joiner.toString();
    }

    private static String formatDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        details.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    private static void append(StringJoiner joiner, String key, Object value) {
        if (value != null) {
            joiner.add(key + "=" + value);
        }
    }
}
