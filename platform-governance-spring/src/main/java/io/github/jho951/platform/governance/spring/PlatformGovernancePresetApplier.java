package io.github.jho951.platform.governance.spring;

import io.github.jho951.platform.governance.api.AuditFailurePolicy;
import io.github.jho951.platform.governance.api.ViolationAction;

import java.util.Objects;

public final class PlatformGovernancePresetApplier {
    public void apply(PlatformGovernanceProperties properties) {
        Objects.requireNonNull(properties, "properties");
        if (!properties.isEnabled()) {
            return;
        }
        GovernanceServiceRolePreset preset = properties.getServiceRolePreset();
        if (preset == null || preset == GovernanceServiceRolePreset.GENERAL) {
            return;
        }
        switch (preset) {
            case IDENTITY_SERVICE -> applyIdentityService(properties);
            case POLICY_DECISION_SERVICE -> applyEnforcingService(properties);
            case RESOURCE_SERVICE -> applyEnforcingService(properties);
            case OBSERVABILITY_SERVICE -> applyObservabilityService(properties);
            case GENERAL -> {
            }
        }
    }

    private void applyIdentityService(PlatformGovernanceProperties properties) {
        ensureIdentityAudit(properties);
        if (!properties.getAudit().isFailurePolicyConfigured()) {
            properties.getAudit().applyFailurePolicy(AuditFailurePolicy.FAIL_CLOSED);
        }
        if (!properties.getOperational().isAllowNonStrictEngineInProductionConfigured()) {
            properties.getOperational().applyAllowNonStrictEngineInProduction(true);
        }
        if (!properties.getOperational().isRequirePolicyConfigInEnforcingModeConfigured()) {
            properties.getOperational().applyRequirePolicyConfigInEnforcingMode(false);
        }
        if (!properties.getViolation().isHandlerFailureFatalConfigured()) {
            properties.getViolation().applyHandlerFailureFatal(true);
        }
    }

    private void applyEnforcingService(PlatformGovernanceProperties properties) {
        if (!properties.getEngine().isStrictConfigured()) {
            properties.getEngine().applyStrict(true);
        }
        if (!properties.getViolation().isActionConfigured()) {
            properties.getViolation().applyAction(ViolationAction.DENY);
        }
        if (!properties.getViolation().isHandlerFailureFatalConfigured()) {
            properties.getViolation().applyHandlerFailureFatal(true);
        }
    }

    private void applyObservabilityService(PlatformGovernanceProperties properties) {
        if (!properties.getOperational().isAllowNonStrictEngineInProductionConfigured()) {
            properties.getOperational().applyAllowNonStrictEngineInProduction(true);
        }
        if (!properties.getOperational().isAllowPermissiveViolationActionInProductionConfigured()) {
            properties.getOperational().applyAllowPermissiveViolationActionInProduction(true);
        }
        if (!properties.getOperational().isRequirePolicyConfigInEnforcingModeConfigured()) {
            properties.getOperational().applyRequirePolicyConfigInEnforcingMode(false);
        }
        if (!properties.getOperational().isRequireFatalHandlerFailuresInProductionConfigured()) {
            properties.getOperational().applyRequireFatalHandlerFailuresInProduction(false);
        }
        if (!properties.getViolation().isActionConfigured()) {
            properties.getViolation().applyAction(ViolationAction.ALERT);
        }
    }

    private void ensureIdentityAudit(PlatformGovernanceProperties properties) {
        PlatformGovernanceProperties.Audit.Identity identity = properties.getAudit().getIdentity();
        if (!identity.isEnabledConfigured()) {
            identity.applyEnabled(true);
        }
        if (!identity.isValidationEnabledConfigured()) {
            identity.applyValidationEnabled(true);
        }
        if (!identity.isFailOnValidationErrorConfigured()) {
            identity.applyFailOnValidationError(true);
        }
    }
}
