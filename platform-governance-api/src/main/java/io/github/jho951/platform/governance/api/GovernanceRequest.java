package io.github.jho951.platform.governance.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record GovernanceRequest(
        String subject,
        String resource,
        String action,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public GovernanceRequest {
        subject = blankToNull(subject);
        resource = requireText(resource, "resource");
        action = requireText(action, "action");
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

