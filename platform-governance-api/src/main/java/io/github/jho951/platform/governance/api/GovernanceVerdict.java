package io.github.jho951.platform.governance.api;

import java.util.Objects;

public record GovernanceVerdict(GovernanceDecision decision, String policy, String reason, PolicyEvidence evidence) {
    public GovernanceVerdict(GovernanceDecision decision, String policy, String reason) {
        this(decision, policy, reason, PolicyEvidence.empty());
    }

    public GovernanceVerdict {
        decision = Objects.requireNonNull(decision, "decision");
        policy = policy == null || policy.isBlank() ? "unknown" : policy.trim();
        reason = reason == null || reason.isBlank() ? null : reason.trim();
        evidence = evidence == null ? PolicyEvidence.empty() : evidence;
    }

    public boolean allowed() {
        return decision.allowed();
    }

    public static GovernanceVerdict allow(String policy, String reason) {
        return new GovernanceVerdict(GovernanceDecision.ALLOW, policy, reason);
    }

    public static GovernanceVerdict allow(String policy, String reason, PolicyEvidence evidence) {
        return new GovernanceVerdict(GovernanceDecision.ALLOW, policy, reason, evidence);
    }

    public static GovernanceVerdict deny(String policy, String reason) {
        return new GovernanceVerdict(GovernanceDecision.DENY, policy, reason);
    }

    public static GovernanceVerdict deny(String policy, String reason, PolicyEvidence evidence) {
        return new GovernanceVerdict(GovernanceDecision.DENY, policy, reason, evidence);
    }
}
