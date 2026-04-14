package io.github.jho951.platform.governance.api;

public interface GovernancePolicyService {
    GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context);
}

