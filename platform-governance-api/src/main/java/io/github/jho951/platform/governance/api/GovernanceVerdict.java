package io.github.jho951.platform.governance.api;

import java.util.Objects;

public record GovernanceVerdict(GovernanceDecision decision, String policy, String reason) {
    public GovernanceVerdict {
        decision = Objects.requireNonNull(decision, "decision");
        policy = policy == null || policy.isBlank() ? "unknown" : policy.trim();
        reason = reason == null || reason.isBlank() ? null : reason.trim();
    }

    public boolean allowed() {
        return decision.allowed();
    }

    public static GovernanceVerdict allow(String policy, String reason) {
        return new GovernanceVerdict(GovernanceDecision.ALLOW, policy, reason);
    }

    public static GovernanceVerdict deny(String policy, String reason) {
        return new GovernanceVerdict(GovernanceDecision.DENY, policy, reason);
    }
}

