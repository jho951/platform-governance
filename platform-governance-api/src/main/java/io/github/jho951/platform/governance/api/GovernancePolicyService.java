package io.github.jho951.platform.governance.api;

/**
 * Platform governance wrapper service.
 *
 * <p>Spring consumers should not treat this as an ordinary override point.
 * Replace {@link GovernanceDecisionEngine} or add {@link GovernancePolicyPlugin}
 * beans for service-specific decisions so the platform wrapper can keep audit
 * recording and violation handling intact.</p>
 */
public interface GovernancePolicyService extends GovernanceDecisionEngine {
}
