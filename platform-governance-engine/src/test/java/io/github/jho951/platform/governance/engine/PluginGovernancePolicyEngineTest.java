package io.github.jho951.platform.governance.engine;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecision;
import io.github.jho951.platform.governance.api.GovernanceEngineFailurePolicy;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginGovernancePolicyEngineTest {
    @Test
    void deniesWhenMatchingPluginDenies() {
        GovernancePolicyPlugin plugin = new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "review-policy";
            }

            @Override
            public boolean supports(GovernanceRequest request, GovernanceContext context) {
                return true;
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                return GovernanceVerdict.deny(name(), "review required");
            }
        };

        PluginGovernancePolicyEngine engine = new PluginGovernancePolicyEngine(List.of(plugin), false);
        GovernanceVerdict verdict = engine.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
    }

    @Test
    void failClosedDeniesWhenPluginThrows() {
        PluginGovernancePolicyEngine engine = new PluginGovernancePolicyEngine(
                List.of(throwingPlugin()),
                false,
                GovernanceEngineFailurePolicy.FAIL_CLOSED
        );

        GovernanceVerdict verdict = engine.evaluate(request(), context());

        assertEquals(GovernanceDecision.DENY, verdict.decision());
        assertEquals("throwing-policy", verdict.policy());
        assertEquals("plugin threw exception: java.lang.IllegalStateException", verdict.reason());
    }

    @Test
    void failOpenAllowsWhenPluginThrows() {
        PluginGovernancePolicyEngine engine = new PluginGovernancePolicyEngine(
                List.of(throwingPlugin()),
                false,
                GovernanceEngineFailurePolicy.FAIL_OPEN
        );

        GovernanceVerdict verdict = engine.evaluate(request(), context());

        assertEquals(GovernanceDecision.ALLOW, verdict.decision());
        assertEquals("plugin threw exception: java.lang.IllegalStateException", verdict.reason());
    }

    private static GovernancePolicyPlugin throwingPlugin() {
        return new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "throwing-policy";
            }

            @Override
            public boolean supports(GovernanceRequest request, GovernanceContext context) {
                return true;
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                throw new IllegalStateException("config unavailable");
            }
        };
    }

    private static GovernanceRequest request() {
        return new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static GovernanceContext context() {
        return new GovernanceContext("actor-1", "prod", Map.of());
    }
}
