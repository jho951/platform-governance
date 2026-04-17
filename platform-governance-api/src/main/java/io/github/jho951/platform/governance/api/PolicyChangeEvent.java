package io.github.jho951.platform.governance.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record PolicyChangeEvent(
        String actor,
        String policyKey,
        String previousValue,
        String newValue,
        String reason,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public PolicyChangeEvent {
        actor = blankToNull(actor);
        policyKey = requireText(policyKey, "policyKey");
        previousValue = blankToNull(previousValue);
        newValue = blankToNull(newValue);
        reason = blankToNull(reason);
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
