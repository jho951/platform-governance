package io.github.jho951.platform.governance.api;

public interface GovernancePolicyPlugin extends GovernancePolicy {
    boolean supports(GovernanceRequest request, GovernanceContext context);
}

