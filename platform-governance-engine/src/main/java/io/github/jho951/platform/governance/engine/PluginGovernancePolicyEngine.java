package io.github.jho951.platform.governance.engine;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecisionEngine;
import io.github.jho951.platform.governance.api.GovernanceEngineFailurePolicy;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;

import java.util.List;
import java.util.Objects;

public final class PluginGovernancePolicyEngine implements GovernancePolicyService, GovernanceDecisionEngine {
    private final List<GovernancePolicyPlugin> plugins;
    private final boolean strict;
    private final GovernanceEngineFailurePolicy failurePolicy;

    public PluginGovernancePolicyEngine(List<GovernancePolicyPlugin> plugins, boolean strict) {
        this(plugins, strict, GovernanceEngineFailurePolicy.FAIL_CLOSED);
    }

    public PluginGovernancePolicyEngine(
            List<GovernancePolicyPlugin> plugins,
            boolean strict,
            GovernanceEngineFailurePolicy failurePolicy
    ) {
        this.plugins = plugins == null ? List.of() : List.copyOf(plugins);
        this.strict = strict;
        this.failurePolicy = failurePolicy == null ? GovernanceEngineFailurePolicy.FAIL_CLOSED : failurePolicy;
    }

    @Override
    public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");

        boolean matched = false;
        for (GovernancePolicyPlugin plugin : plugins) {
            try {
                if (!plugin.supports(request, context)) {
                    continue;
                }
                matched = true;
                GovernanceVerdict verdict = plugin.evaluate(request, context);
                if (!verdict.allowed()) {
                    return verdict;
                }
            } catch (RuntimeException exception) {
                return handlePluginFailure(plugin, exception);
            }
        }

        if (!matched && strict) {
            return GovernanceVerdict.deny("plugin-engine", "no matching plugin");
        }
        return GovernanceVerdict.allow("plugin-engine", matched ? "all matching plugins passed" : "no plugin matched");
    }

    private GovernanceVerdict handlePluginFailure(GovernancePolicyPlugin plugin, RuntimeException exception) {
        String pluginName = plugin == null ? "unknown" : plugin.name();
        String reason = "plugin threw exception: " + exception.getClass().getName();
        if (failurePolicy == GovernanceEngineFailurePolicy.FAIL_OPEN) {
            return GovernanceVerdict.allow(pluginName, reason);
        }
        return GovernanceVerdict.deny(pluginName, reason);
    }
}
