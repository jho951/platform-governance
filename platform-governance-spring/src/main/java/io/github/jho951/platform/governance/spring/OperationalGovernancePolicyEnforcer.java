package io.github.jho951.platform.governance.spring;

import io.github.jho951.platform.governance.api.AuditFailurePolicy;
import io.github.jho951.platform.governance.api.OperationalProfileResolver;
import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.api.ViolationAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class OperationalGovernancePolicyEnforcer {
    private final PlatformGovernanceProperties properties;
    private final PolicyConfigSource policyConfigSource;
    private final OperationalProfileResolver operationalProfileResolver;
    private final List<String> activeProfiles;
    private final int auditSinkCount;
    private final int auditContextResolverCount;

    public OperationalGovernancePolicyEnforcer(
            PlatformGovernanceProperties properties,
            PolicyConfigSource policyConfigSource,
            String[] activeProfiles,
            int auditSinkCount
    ) {
        this(properties, policyConfigSource, activeProfiles, auditSinkCount, 0);
    }

    public OperationalGovernancePolicyEnforcer(
            PlatformGovernanceProperties properties,
            PolicyConfigSource policyConfigSource,
            String[] activeProfiles,
            int auditSinkCount,
            int auditContextResolverCount
    ) {
        this(
                properties,
                policyConfigSource,
                OperationalProfileResolver.standard(),
                activeProfiles,
                auditSinkCount,
                auditContextResolverCount
        );
    }

    public OperationalGovernancePolicyEnforcer(
            PlatformGovernanceProperties properties,
            PolicyConfigSource policyConfigSource,
            OperationalProfileResolver operationalProfileResolver,
            String[] activeProfiles,
            int auditSinkCount,
            int auditContextResolverCount
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.policyConfigSource = Objects.requireNonNull(policyConfigSource, "policyConfigSource");
        this.operationalProfileResolver = Objects.requireNonNull(operationalProfileResolver, "operationalProfileResolver");
        this.activeProfiles = activeProfiles == null ? List.of() : Arrays.asList(activeProfiles);
        this.auditSinkCount = Math.max(0, auditSinkCount);
        this.auditContextResolverCount = Math.max(0, auditContextResolverCount);
    }

    public void enforce() {
        List<String> violations = new ArrayList<>();
        validateAlways(violations);
        throwIfInvalid(violations);

        PlatformGovernanceProperties.Operational operational = properties.getOperational();
        if (!operational.isFailFastEnabled()) {
            return;
        }

        validateRuntimeProfileConsistency(violations);

        if (isProductionProfileActive()) {
            validateProductionSafety(operational, violations);
        }

        throwIfInvalid(violations);
    }

    private static void throwIfInvalid(List<String> violations) {
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Invalid platform.governance operational configuration: " + String.join("; ", violations));
        }
    }

    private void validateRuntimeProfileConsistency(List<String> violations) {
        String auditEnvironment = properties.getAudit().getEnvironment();
        if (isBlank(auditEnvironment) || activeProfiles.isEmpty()) {
            return;
        }
        boolean environmentIsProduction = operationalProfileResolver.isProduction(
                List.of(auditEnvironment),
                properties.getOperational().getProductionProfiles()
        );
        if (isProductionProfileActive() != environmentIsProduction) {
            violations.add("audit.environment=" + auditEnvironment + " conflicts with active profiles "
                    + activeProfiles);
        }
    }

    private boolean isProductionProfileActive() {
        return operationalProfileResolver.isProduction(activeProfiles, properties.getOperational().getProductionProfiles());
    }

    private void validateAlways(List<String> violations) {
        if (properties.hasMixedFeatureFlagPrefixes()) {
            violations.add("Detected both platform.governance.feature-flags.* and legacy "
                    + "platform.governance.plugin-policy-engine.*. Mixed configuration is not supported. "
                    + "Use exactly one prefix.");
            return;
        }
        PlatformGovernanceProperties.FeatureFlags featureFlags = properties.effectiveFeatureFlags();
        if (featureFlags.getStore() == PlatformGovernanceProperties.FeatureFlags.Store.FILE
                && (featureFlags.getFilePath() == null || featureFlags.getFilePath().isBlank())) {
            violations.add("feature-flags.store=FILE requires feature-flags.file-path");
        }
    }

    private void validateProductionSafety(PlatformGovernanceProperties.Operational operational, List<String> violations) {
        if (!properties.getAudit().isEnabled() && !operational.isAllowAuditDisabledInProduction()) {
            violations.add("audit.enabled=false is not allowed in production profiles");
        }
        if (!properties.getEngine().isStrict() && !operational.isAllowNonStrictEngineInProduction()) {
            violations.add("engine.strict=false is not allowed in production profiles");
        }
        if (isWeakViolationResponse(properties.getViolation().getAction()) && !operational.isAllowPermissiveViolationActionInProduction()) {
            violations.add("violation.action=" + properties.getViolation().getAction() + " is not allowed in production profiles");
        }
        if (properties.getAudit().isEnabled() && operational.isRequireAuditSinkInProduction() && auditSinkCount == 0) {
            violations.add("at least one AuditSink bean is required in production profiles");
        }
        if (properties.getAudit().isEnabled()
                && operational.isRequireAuditContextResolverInProduction()
                && auditContextResolverCount == 0) {
            violations.add("at least one AuditContextResolver bean is required in production profiles");
        }
        if (properties.getAudit().isEnabled()
                && operational.isRequireAuditServiceIdentityInProduction()
                && (isBlank(properties.getAudit().getServiceName()) || isBlank(properties.getAudit().getEnvironment()))) {
            violations.add("audit.service-name and audit.environment are required in production profiles");
        }
        if (properties.getAudit().isEnabled()
                && operational.isRequireIdentityAuditValidationInProduction()
                && !properties.getAudit().getIdentity().isValidationEnabled()) {
            violations.add("audit.identity.validation-enabled=false is not allowed in production profiles");
        }
        if (properties.getAudit().isEnabled()
                && properties.getAudit().getFailurePolicy() == AuditFailurePolicy.IGNORE
                && !operational.isAllowIgnoreAuditFailurePolicyInProduction()) {
            violations.add("audit.failure-policy=IGNORE is not allowed in production profiles");
        }
        PolicyConfigOperationalStatus policyConfigStatus = policyConfigSource.operationalStatus();
        if (operational.isRequirePolicyConfigInEnforcingMode()
                && isDenyResponse(properties.getViolation().getAction())
                && !policyConfigStatus.isOperational()) {
            violations.add("policy config source must be operational when violation.action is enforcing in production profiles"
                    + " (status=" + policyConfigStatus + ")");
        }
        if (operational.isRequirePolicyConfigInEnforcingMode()
                && isDenyResponse(properties.getViolation().getAction())
                && policyConfigSource.supportsSnapshot()
                && policyConfigSource.snapshot().isEmpty()) {
            violations.add("policy config source snapshot must not be empty when violation.action is enforcing in production profiles");
        }
        if (operational.isRequireFatalHandlerFailuresInProduction()
                && !properties.getViolation().isHandlerFailureFatal()) {
            violations.add("violation.handler-failure-fatal=false is not allowed in production profiles");
        }
    }

    private static boolean isWeakViolationResponse(ViolationAction action) {
        return action == ViolationAction.AUDIT_ONLY || action == ViolationAction.ALERT;
    }

    private static boolean isDenyResponse(ViolationAction action) {
        return action == ViolationAction.DENY || action == ViolationAction.ESCALATE;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
