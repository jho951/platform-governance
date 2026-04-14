package io.github.jho951.platform.governance.core;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecision;
import io.github.jho951.platform.governance.api.GovernancePolicy;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultGovernancePolicyServiceTest {
    @Test
    void deniesWhenAnyPolicyDenies() {
        GovernancePolicy policy = new GovernancePolicy() {
            @Override
            public String name() {
                return "policy";
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                return GovernanceVerdict.deny(name(), "blocked");
            }
        };
        DefaultGovernancePolicyService service = new DefaultGovernancePolicyService(List.of(policy));

        GovernanceVerdict verdict = service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
    }
}
