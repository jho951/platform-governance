package io.github.jho951.platform.governance.api;

public interface ViolationHandler {
    void handle(GovernanceViolation violation);
}
