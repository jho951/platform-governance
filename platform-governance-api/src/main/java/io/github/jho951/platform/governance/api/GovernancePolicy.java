package io.github.jho951.platform.governance.api;

public interface GovernancePolicy {
    String name();

    GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context);
}

