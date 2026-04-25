package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditSink;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes governance audit entries to the application log so local and test
 * environments do not silently drop audit output when no external sink is
 * configured.
 */
public final class LoggingAuditSink implements GovernanceAuditSink {
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
    public void write(AuditEntry entry) {
        logger.log(level, () -> format(entry));
    }

    private static String format(AuditEntry entry) {
        StringJoiner joiner = new StringJoiner(", ", "governance-audit{", "}");
        append(joiner, "category", entry.category());
        append(joiner, "message", entry.message());
        append(joiner, "occurredAt", entry.occurredAt());
        append(joiner, "attributes", formatAttributes(entry.attributes()));
        return joiner.toString();
    }

    private static String formatAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "{}";
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        attributes.forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    private static void append(StringJoiner joiner, String key, Object value) {
        if (value != null) {
            joiner.add(key + "=" + value);
        }
    }
}
