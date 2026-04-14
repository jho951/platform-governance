package io.github.jho951.platform.governance.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record AuditEntry(
        String category,
        String message,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public AuditEntry {
        category = category == null || category.isBlank() ? "governance" : category.trim();
        message = message == null || message.isBlank() ? "" : message.trim();
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}

