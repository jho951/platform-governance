package io.github.jho951.platform.governance.test;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceRequest;

import java.time.Instant;
import java.util.Map;

public final class PlatformGovernanceFixtures {
    private PlatformGovernanceFixtures() {
    }

    public static GovernanceRequest sampleRequest() {
        return new GovernanceRequest(
                "user-1",
                "/governance/review",
                "review",
                Map.of("source", "test"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    public static GovernanceContext sampleContext() {
        return new GovernanceContext("actor-1", "prod", Map.of());
    }
}

