package io.github.jho951.platform.governance.api;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record GovernanceViolation(
        GovernanceRequest request,
        GovernanceContext context,
        GovernanceVerdict verdict,
        ViolationAction action,
        Map<String, String> attributes,
        Instant occurredAt
) {
    public GovernanceViolation {
        request = Objects.requireNonNull(request, "request");
        context = Objects.requireNonNull(context, "context");
        verdict = Objects.requireNonNull(verdict, "verdict");
        action = action == null ? ViolationAction.DENY : action;
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
        occurredAt = occurredAt == null ? request.occurredAt() : occurredAt;
    }
}
