package io.github.jho951.platform.governance.api;

public enum GovernanceDecision {
    ALLOW,
    DENY;

    public boolean allowed() {
        return this == ALLOW;
    }
}

