package io.github.jho951.platform.governance.engine;

import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecision;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;

import java.util.List;
import java.util.Objects;

public final class PluginGovernancePolicyEngine implements GovernancePolicyService {
    private final List<GovernancePolicyPlugin> plugins;
    private final boolean strict;

    public PluginGovernancePolicyEngine(List<GovernancePolicyPlugin> plugins, boolean strict) {
        this.plugins = plugins == null ? List.of() : List.copyOf(plugins);
        this.strict = strict;
    }

    @Override
    public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");

        boolean matched = false;
        for (GovernancePolicyPlugin plugin : plugins) {
            if (!plugin.supports(request, context)) {
                continue;
            }
            matched = true;
            GovernanceVerdict verdict = plugin.evaluate(request, context);
            if (!verdict.allowed()) {
                return verdict;
            }
        }

        if (!matched && strict) {
            return GovernanceVerdict.deny("plugin-engine", "no matching plugin");
        }
        return GovernanceVerdict.allow("plugin-engine", matched ? "all matching plugins passed" : "no plugin matched");
    }
}
