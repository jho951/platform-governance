package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditSink;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditSink;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adapts platform-owned governance sinks to the raw audit-log sink used by the
 * internal audit logger.
 */
public final class GovernanceAuditSinkAuditSinkAdapter implements AuditSink {
    private final GovernanceAuditSink delegate;

    public GovernanceAuditSinkAuditSinkAdapter(GovernanceAuditSink delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void write(AuditEvent event) {
        delegate.write(toAuditEntry(event));
    }

    private static AuditEntry toAuditEntry(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        return new AuditEntry(
                resolveCategory(event),
                resolveMessage(event),
                attributes(event),
                event.getOccurredAt()
        );
    }

    private static String resolveCategory(AuditEvent event) {
        Object detailedCategory = event.getDetails() == null ? null : event.getDetails().get("category");
        if (detailedCategory != null && !detailedCategory.toString().isBlank()) {
            return detailedCategory.toString();
        }
        if (event.getResourceType() != null && !event.getResourceType().isBlank()) {
            return event.getResourceType();
        }
        if (event.getResourceId() != null && !event.getResourceId().isBlank()) {
            return event.getResourceId();
        }
        return "governance";
    }

    private static String resolveMessage(AuditEvent event) {
        if (event.getAction() != null && !event.getAction().isBlank()) {
            return event.getAction();
        }
        if (event.getReason() != null && !event.getReason().isBlank()) {
            return event.getReason();
        }
        return event.getEventType() == null ? "" : event.getEventType().name();
    }

    private static Map<String, String> attributes(AuditEvent event) {
        Map<String, String> attributes = new LinkedHashMap<>();
        put(attributes, "audit.event-id", event.getEventId());
        put(attributes, "audit.actor-id", event.getActorId());
        put(attributes, "audit.actor-type", event.getActorType());
        put(attributes, "audit.actor-name", event.getActorName());
        put(attributes, "audit.event-type", event.getEventType());
        put(attributes, "audit.action", event.getAction());
        put(attributes, "audit.resource-type", event.getResourceType());
        put(attributes, "audit.resource-id", event.getResourceId());
        put(attributes, "audit.result", event.getResult());
        put(attributes, "audit.reason", event.getReason());
        put(attributes, "audit.trace-id", event.getTraceId());
        put(attributes, "audit.request-id", event.getRequestId());
        put(attributes, "audit.client-ip", event.getClientIp());
        put(attributes, "audit.user-agent", event.getUserAgent());
        if (event.getDetails() != null) {
            event.getDetails().forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    attributes.put(key, value.toString());
                }
            });
        }
        return attributes;
    }

    private static void put(Map<String, String> attributes, String key, Object value) {
        if (key != null && !key.isBlank() && value != null) {
            attributes.put(key, value.toString());
        }
    }
}
