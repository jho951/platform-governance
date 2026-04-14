package io.github.jho951.platform.governance.api;

import java.util.Collections;
import java.util.Map;

public record GovernanceContext(
        String actor,
        String environment,
        Map<String, String> attributes
) {
    public GovernanceContext {
        actor = actor == null || actor.isBlank() ? null : actor.trim();
        environment = environment == null || environment.isBlank() ? null : environment.trim();
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
    }
}

