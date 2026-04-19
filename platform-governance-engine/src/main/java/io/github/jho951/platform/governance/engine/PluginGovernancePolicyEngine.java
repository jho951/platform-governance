package io.github.jho951.platform.governance.engine;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecisionEngine;
import io.github.jho951.platform.governance.api.GovernanceEngineFailurePolicy;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;

import java.util.List;

/**
 * @deprecated since 2.0.1. Use {@link PluginGovernanceDecisionEngine} as the
 * decision engine and let the Spring platform wrapper expose
 * {@link GovernancePolicyService}. This compatibility adapter will be removed
 * in 3.0.0.
 */
@Deprecated(since = "2.0.1", forRemoval = true)
public final class PluginGovernancePolicyEngine implements GovernancePolicyService, GovernanceDecisionEngine {
    private final PluginGovernanceDecisionEngine delegate;

    public PluginGovernancePolicyEngine(List<GovernancePolicyPlugin> plugins, boolean strict) {
        this(plugins, strict, GovernanceEngineFailurePolicy.FAIL_CLOSED);
    }

    public PluginGovernancePolicyEngine(
            List<GovernancePolicyPlugin> plugins,
            boolean strict,
            GovernanceEngineFailurePolicy failurePolicy
    ) {
        this.delegate = new PluginGovernanceDecisionEngine(plugins, strict, failurePolicy);
    }

    @Override
    public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
        return delegate.evaluate(request, context);
    }
}
