package io.github.jho951.platform.governance.api;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GovernanceRequestTest {
    @Test
    void normalizesValues() {
        GovernanceRequest request = new GovernanceRequest(
                " user-1 ",
                " resource-1 ",
                " review ",
                Map.of("k", "v"),
                Instant.parse("2026-01-01T00:00:00Z")
        );

        assertEquals("user-1", request.subject());
        assertEquals("resource-1", request.resource());
        assertEquals("review", request.action());
    }

    @Test
    void defaultContextCollectionsAreEmpty() {
        GovernanceContext context = new GovernanceContext("  ", " prod ", null);

        assertNull(context.actor());
        assertEquals("prod", context.environment());
    }
}
