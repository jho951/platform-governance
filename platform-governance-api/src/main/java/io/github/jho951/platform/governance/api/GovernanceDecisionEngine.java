package io.github.jho951.platform.governance.api;

public interface GovernanceDecisionEngine {
    GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context);
}
