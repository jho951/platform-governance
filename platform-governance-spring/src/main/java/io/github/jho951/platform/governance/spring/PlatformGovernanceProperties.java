package io.github.jho951.platform.governance.spring;

import io.github.jho951.platform.governance.api.AuditFailurePolicy;
import io.github.jho951.platform.governance.api.GovernanceEngineFailurePolicy;
import io.github.jho951.platform.governance.api.ViolationAction;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "platform.governance")
public class PlatformGovernanceProperties {
    private boolean enabled = true;
    private GovernanceServiceRolePreset serviceRolePreset = GovernanceServiceRolePreset.GENERAL;
    private final Audit audit = new Audit();
    private final PolicyConfig policyConfig = new PolicyConfig();
    private final FeatureFlags featureFlags = new FeatureFlags();
    private final PluginPolicyEngine pluginPolicyEngine = new PluginPolicyEngine();
    private final Engine engine = new Engine();
    private final Violation violation = new Violation();
    private final Operational operational = new Operational();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GovernanceServiceRolePreset getServiceRolePreset() {
        return serviceRolePreset;
    }

    public void setServiceRolePreset(GovernanceServiceRolePreset serviceRolePreset) {
        this.serviceRolePreset = serviceRolePreset == null ? GovernanceServiceRolePreset.GENERAL : serviceRolePreset;
    }

    public Audit getAudit() {
        return audit;
    }

    public PolicyConfig getPolicyConfig() {
        return policyConfig;
    }

    public FeatureFlags getFeatureFlags() {
        return featureFlags;
    }

    /**
     * @deprecated since 2.0.1. Use {@link #getFeatureFlags()} and the
     * {@code platform.governance.feature-flags.*} prefix. The legacy
     * {@code platform.governance.plugin-policy-engine.*} prefix will be removed
     * in 3.1.0.
     */
    @Deprecated(since = "2.0.1", forRemoval = true)
    public PluginPolicyEngine getPluginPolicyEngine() {
        return pluginPolicyEngine;
    }

    public Engine getEngine() {
        return engine;
    }

    public Violation getViolation() {
        return violation;
    }

    public Operational getOperational() {
        return operational;
    }

    public static class Audit {
        private boolean enabled = true;
        private String serviceName;
        private String environment;
        private AuditFailurePolicy failurePolicy = AuditFailurePolicy.FAIL_CLOSED;
        private boolean failurePolicyConfigured = false;
        private final Identity identity = new Identity();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public AuditFailurePolicy getFailurePolicy() {
            return failurePolicy;
        }

        public void setFailurePolicy(AuditFailurePolicy failurePolicy) {
            this.failurePolicyConfigured = true;
            this.failurePolicy = failurePolicy == null ? AuditFailurePolicy.FAIL_CLOSED : failurePolicy;
        }

        boolean isFailurePolicyConfigured() {
            return failurePolicyConfigured;
        }

        void applyFailurePolicy(AuditFailurePolicy failurePolicy) {
            this.failurePolicy = failurePolicy == null ? AuditFailurePolicy.FAIL_CLOSED : failurePolicy;
        }

        public Identity getIdentity() {
            return identity;
        }

        public static class Identity {
            private boolean enabled = true;
            private boolean validationEnabled = true;
            private boolean failOnValidationError = true;
            private boolean enabledConfigured = false;
            private boolean validationEnabledConfigured = false;
            private boolean failOnValidationErrorConfigured = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabledConfigured = true;
                this.enabled = enabled;
            }

            boolean isEnabledConfigured() {
                return enabledConfigured;
            }

            void applyEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isValidationEnabled() {
                return validationEnabled;
            }

            public void setValidationEnabled(boolean validationEnabled) {
                this.validationEnabledConfigured = true;
                this.validationEnabled = validationEnabled;
            }

            boolean isValidationEnabledConfigured() {
                return validationEnabledConfigured;
            }

            void applyValidationEnabled(boolean validationEnabled) {
                this.validationEnabled = validationEnabled;
            }

            public boolean isFailOnValidationError() {
                return failOnValidationError;
            }

            public void setFailOnValidationError(boolean failOnValidationError) {
                this.failOnValidationErrorConfigured = true;
                this.failOnValidationError = failOnValidationError;
            }

            boolean isFailOnValidationErrorConfigured() {
                return failOnValidationErrorConfigured;
            }

            void applyFailOnValidationError(boolean failOnValidationError) {
                this.failOnValidationError = failOnValidationError;
            }
        }
    }

    public static class PolicyConfig {
        private Map<String, String> values = new LinkedHashMap<>();

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values == null ? new LinkedHashMap<>() : values;
        }
    }

    FeatureFlags effectiveFeatureFlags() {
        FeatureFlags effective = new FeatureFlags();
        effective.applyStore(featureFlags.isStoreConfigured() ? featureFlags.getStore() : pluginPolicyEngine.getStore());
        effective.applyFilePath(featureFlags.isFilePathConfigured() ? featureFlags.getFilePath() : pluginPolicyEngine.getFilePath());
        effective.applyCacheTtlMillis(featureFlags.isCacheTtlMillisConfigured()
                ? featureFlags.getCacheTtlMillis()
                : pluginPolicyEngine.getCacheTtlMillis());
        return effective;
    }

    boolean hasMixedFeatureFlagPrefixes() {
        return featureFlags.isConfigured() && pluginPolicyEngine.isConfigured();
    }

    public static class FeatureFlags {
        private Store store = Store.MEMORY;
        private String filePath;
        private long cacheTtlMillis = 3000;
        private boolean storeConfigured = false;
        private boolean filePathConfigured = false;
        private boolean cacheTtlMillisConfigured = false;

        public Store getStore() {
            return store;
        }

        public void setStore(Store store) {
            this.storeConfigured = true;
            this.store = store == null ? Store.MEMORY : store;
        }

        boolean isStoreConfigured() {
            return storeConfigured;
        }

        void applyStore(Store store) {
            this.store = store == null ? Store.MEMORY : store;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePathConfigured = true;
            this.filePath = filePath;
        }

        boolean isFilePathConfigured() {
            return filePathConfigured;
        }

        void applyFilePath(String filePath) {
            this.filePath = filePath;
        }

        public long getCacheTtlMillis() {
            return cacheTtlMillis;
        }

        public void setCacheTtlMillis(long cacheTtlMillis) {
            this.cacheTtlMillisConfigured = true;
            this.cacheTtlMillis = cacheTtlMillis < 0 ? 0 : cacheTtlMillis;
        }

        boolean isCacheTtlMillisConfigured() {
            return cacheTtlMillisConfigured;
        }

        void applyCacheTtlMillis(long cacheTtlMillis) {
            this.cacheTtlMillis = cacheTtlMillis < 0 ? 0 : cacheTtlMillis;
        }

        boolean isConfigured() {
            return storeConfigured || filePathConfigured || cacheTtlMillisConfigured;
        }

        public enum Store {
            MEMORY,
            FILE
        }
    }

    /**
     * @deprecated since 2.0.1. Use {@link FeatureFlags}. This class remains
     * only for the legacy {@code platform.governance.plugin-policy-engine.*}
     * prefix and will be removed in 3.1.0.
     */
    @Deprecated(since = "2.0.1", forRemoval = true)
    public static class PluginPolicyEngine extends FeatureFlags {
    }

    public static class Engine {
        private boolean strict = false;
        private boolean strictConfigured = false;
        private GovernanceEngineFailurePolicy failurePolicy = GovernanceEngineFailurePolicy.FAIL_CLOSED;

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strictConfigured = true;
            this.strict = strict;
        }

        boolean isStrictConfigured() {
            return strictConfigured;
        }

        void applyStrict(boolean strict) {
            this.strict = strict;
        }

        public GovernanceEngineFailurePolicy getFailurePolicy() {
            return failurePolicy;
        }

        public void setFailurePolicy(GovernanceEngineFailurePolicy failurePolicy) {
            this.failurePolicy = failurePolicy == null ? GovernanceEngineFailurePolicy.FAIL_CLOSED : failurePolicy;
        }
    }

    public static class Violation {
        private ViolationAction action = ViolationAction.DENY;
        private boolean handlerFailureFatal = false;
        private boolean actionConfigured = false;
        private boolean handlerFailureFatalConfigured = false;

        public ViolationAction getAction() {
            return action;
        }

        public void setAction(ViolationAction action) {
            this.actionConfigured = true;
            this.action = action == null ? ViolationAction.DENY : action;
        }

        boolean isActionConfigured() {
            return actionConfigured;
        }

        void applyAction(ViolationAction action) {
            this.action = action == null ? ViolationAction.DENY : action;
        }

        public boolean isHandlerFailureFatal() {
            return handlerFailureFatal;
        }

        public void setHandlerFailureFatal(boolean handlerFailureFatal) {
            this.handlerFailureFatalConfigured = true;
            this.handlerFailureFatal = handlerFailureFatal;
        }

        boolean isHandlerFailureFatalConfigured() {
            return handlerFailureFatalConfigured;
        }

        void applyHandlerFailureFatal(boolean handlerFailureFatal) {
            this.handlerFailureFatal = handlerFailureFatal;
        }
    }

    public static class Operational {
        private boolean failFastEnabled = true;
        private Set<String> productionProfiles = new LinkedHashSet<>(Set.of("prod", "production"));
        private boolean allowAuditDisabledInProduction = false;
        private boolean allowNonStrictEngineInProduction = false;
        private boolean allowPermissiveViolationActionInProduction = false;
        private boolean requireAuditSinkInProduction = true;
        private boolean requireAuditContextResolverInProduction = true;
        private boolean requireAuditServiceIdentityInProduction = true;
        private boolean requireIdentityAuditValidationInProduction = true;
        private boolean requirePolicyConfigInEnforcingMode = true;
        private boolean requireFatalHandlerFailuresInProduction = true;
        private boolean allowIgnoreAuditFailurePolicyInProduction = false;
        private boolean allowNonStrictEngineInProductionConfigured = false;
        private boolean allowPermissiveViolationActionInProductionConfigured = false;
        private boolean requirePolicyConfigInEnforcingModeConfigured = false;
        private boolean requireFatalHandlerFailuresInProductionConfigured = false;

        public boolean isFailFastEnabled() {
            return failFastEnabled;
        }

        public void setFailFastEnabled(boolean failFastEnabled) {
            this.failFastEnabled = failFastEnabled;
        }

        public Set<String> getProductionProfiles() {
            return productionProfiles;
        }

        public void setProductionProfiles(Set<String> productionProfiles) {
            this.productionProfiles = productionProfiles == null ? new LinkedHashSet<>() : productionProfiles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(profile -> !profile.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        public boolean isAllowAuditDisabledInProduction() {
            return allowAuditDisabledInProduction;
        }

        public void setAllowAuditDisabledInProduction(boolean allowAuditDisabledInProduction) {
            this.allowAuditDisabledInProduction = allowAuditDisabledInProduction;
        }

        public boolean isAllowNonStrictEngineInProduction() {
            return allowNonStrictEngineInProduction;
        }

        public void setAllowNonStrictEngineInProduction(boolean allowNonStrictEngineInProduction) {
            this.allowNonStrictEngineInProductionConfigured = true;
            this.allowNonStrictEngineInProduction = allowNonStrictEngineInProduction;
        }

        boolean isAllowNonStrictEngineInProductionConfigured() {
            return allowNonStrictEngineInProductionConfigured;
        }

        void applyAllowNonStrictEngineInProduction(boolean allowNonStrictEngineInProduction) {
            this.allowNonStrictEngineInProduction = allowNonStrictEngineInProduction;
        }

        public boolean isAllowPermissiveViolationActionInProduction() {
            return allowPermissiveViolationActionInProduction;
        }

        public void setAllowPermissiveViolationActionInProduction(boolean allowPermissiveViolationActionInProduction) {
            this.allowPermissiveViolationActionInProductionConfigured = true;
            this.allowPermissiveViolationActionInProduction = allowPermissiveViolationActionInProduction;
        }

        boolean isAllowPermissiveViolationActionInProductionConfigured() {
            return allowPermissiveViolationActionInProductionConfigured;
        }

        void applyAllowPermissiveViolationActionInProduction(boolean allowPermissiveViolationActionInProduction) {
            this.allowPermissiveViolationActionInProduction = allowPermissiveViolationActionInProduction;
        }

        public boolean isRequireAuditSinkInProduction() {
            return requireAuditSinkInProduction;
        }

        public void setRequireAuditSinkInProduction(boolean requireAuditSinkInProduction) {
            this.requireAuditSinkInProduction = requireAuditSinkInProduction;
        }

        public boolean isRequireAuditContextResolverInProduction() {
            return requireAuditContextResolverInProduction;
        }

        public void setRequireAuditContextResolverInProduction(boolean requireAuditContextResolverInProduction) {
            this.requireAuditContextResolverInProduction = requireAuditContextResolverInProduction;
        }

        public boolean isRequireAuditServiceIdentityInProduction() {
            return requireAuditServiceIdentityInProduction;
        }

        public void setRequireAuditServiceIdentityInProduction(boolean requireAuditServiceIdentityInProduction) {
            this.requireAuditServiceIdentityInProduction = requireAuditServiceIdentityInProduction;
        }

        public boolean isRequireIdentityAuditValidationInProduction() {
            return requireIdentityAuditValidationInProduction;
        }

        public void setRequireIdentityAuditValidationInProduction(boolean requireIdentityAuditValidationInProduction) {
            this.requireIdentityAuditValidationInProduction = requireIdentityAuditValidationInProduction;
        }

        public boolean isRequirePolicyConfigInEnforcingMode() {
            return requirePolicyConfigInEnforcingMode;
        }

        public void setRequirePolicyConfigInEnforcingMode(boolean requirePolicyConfigInEnforcingMode) {
            this.requirePolicyConfigInEnforcingModeConfigured = true;
            this.requirePolicyConfigInEnforcingMode = requirePolicyConfigInEnforcingMode;
        }

        boolean isRequirePolicyConfigInEnforcingModeConfigured() {
            return requirePolicyConfigInEnforcingModeConfigured;
        }

        void applyRequirePolicyConfigInEnforcingMode(boolean requirePolicyConfigInEnforcingMode) {
            this.requirePolicyConfigInEnforcingMode = requirePolicyConfigInEnforcingMode;
        }

        public boolean isRequireFatalHandlerFailuresInProduction() {
            return requireFatalHandlerFailuresInProduction;
        }

        public void setRequireFatalHandlerFailuresInProduction(boolean requireFatalHandlerFailuresInProduction) {
            this.requireFatalHandlerFailuresInProductionConfigured = true;
            this.requireFatalHandlerFailuresInProduction = requireFatalHandlerFailuresInProduction;
        }

        boolean isRequireFatalHandlerFailuresInProductionConfigured() {
            return requireFatalHandlerFailuresInProductionConfigured;
        }

        void applyRequireFatalHandlerFailuresInProduction(boolean requireFatalHandlerFailuresInProduction) {
            this.requireFatalHandlerFailuresInProduction = requireFatalHandlerFailuresInProduction;
        }

        public boolean isAllowIgnoreAuditFailurePolicyInProduction() {
            return allowIgnoreAuditFailurePolicyInProduction;
        }

        public void setAllowIgnoreAuditFailurePolicyInProduction(boolean allowIgnoreAuditFailurePolicyInProduction) {
            this.allowIgnoreAuditFailurePolicyInProduction = allowIgnoreAuditFailurePolicyInProduction;
        }
    }

}
