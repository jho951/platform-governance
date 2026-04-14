package io.github.jho951.platform.governance.core;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernancePolicy;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;

import java.util.List;
import java.util.Objects;

public final class DefaultGovernancePolicyService implements GovernancePolicyService {
    private final List<GovernancePolicy> policies;

    public DefaultGovernancePolicyService(List<GovernancePolicy> policies) {
        this.policies = policies == null ? List.of() : List.copyOf(policies);
    }

    @Override
    public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");

        for (GovernancePolicy policy : policies) {
            GovernanceVerdict verdict = policy.evaluate(request, context);
            if (!verdict.allowed()) {
                return verdict;
            }
        }
        return GovernanceVerdict.allow("default", "all governance policies passed");
    }
}

