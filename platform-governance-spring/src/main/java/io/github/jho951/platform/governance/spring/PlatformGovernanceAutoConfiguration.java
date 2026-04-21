package io.github.jho951.platform.governance.spring;

import com.auditlog.api.AuditLogger;
import com.auditlog.api.AuditSink;
import com.auditlog.core.CompositeAuditSink;
import com.auditlog.core.DefaultAuditLogger;
import com.auditlog.spi.AuditContextResolver;
import com.auditlog.spi.AuditMaskingPolicy;
import com.pluginpolicyengine.api.FeatureFlagClient;
import com.pluginpolicyengine.config.FeatureFlagClientFactory;
import com.pluginpolicyengine.config.FeatureFlagConfig;
import com.policyconfig.builder.PolicyConfigs;
import com.policyconfig.contracts.PolicyResolver;
import io.github.jho951.platform.governance.api.AuditCorrelation;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.GovernanceDecisionEngine;
import io.github.jho951.platform.governance.api.GovernanceViolation;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import io.github.jho951.platform.governance.api.PolicyEvidence;
import io.github.jho951.platform.governance.api.PolicyChangeRecorder;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.api.OperationalProfileResolver;
import io.github.jho951.platform.governance.api.ViolationHandler;
import io.github.jho951.platform.governance.audit.AuditLoggerAuditLogRecorder;
import io.github.jho951.platform.governance.audit.AuditPolicyChangeRecorder;
import io.github.jho951.platform.governance.audit.AuditViolationHandler;
import io.github.jho951.platform.governance.audit.CompositeAuditLogRecorder;
import io.github.jho951.platform.governance.audit.DefaultIdentityAuditRecorder;
import io.github.jho951.platform.governance.audit.IdentityAuditEventValidator;
import io.github.jho951.platform.governance.audit.LoggingAuditSink;
import io.github.jho951.platform.governance.audit.SanitizingAuditLogRecorder;
import io.github.jho951.platform.governance.config.PolicyResolverPolicyConfigSource;
import io.github.jho951.platform.governance.engine.PluginGovernanceDecisionEngine;
import io.github.jho951.platform.governance.identity.AuditAttributeEnricher;
import io.github.jho951.platform.governance.identity.IdentityAuditCustomizer;
import io.github.jho951.platform.governance.identity.IdentityAuditRecorder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.governance", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PlatformGovernanceProperties.class)
public class PlatformGovernanceAutoConfiguration {
    private static final Log LOGGER = LogFactory.getLog(PlatformGovernanceAutoConfiguration.class);

    @Bean
    public static BeanPostProcessor platformGovernancePropertiesPresetPostProcessor() {
        PlatformGovernancePresetApplier presetApplier = new PlatformGovernancePresetApplier();
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof PlatformGovernanceProperties properties) {
                    presetApplier.apply(properties);
                }
                return bean;
            }
        };
    }

    @Bean
    public static BeanPostProcessor platformGovernancePolicyServiceOverrideGuardPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof GovernancePolicyService && !"platformGovernancePolicyService".equals(beanName)) {
                    throw new BeanCreationException(beanName,
                            "Custom GovernancePolicyService beans are not supported. Use GovernanceDecisionEngine, "
                                    + "PolicyConfigSource, ViolationHandler, or AuditSink instead.");
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock platformGovernanceClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyResolver platformGovernancePolicyResolver(PlatformGovernanceProperties properties) {
        return PolicyConfigs.builder()
                .map(properties.getPolicyConfig().getValues())
                .env()
                .systemProperties()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(PolicyConfigSource.class)
    public PolicyConfigSource policyConfigSource(PolicyResolver policyResolver) {
        return new PolicyResolverPolicyConfigSource(policyResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public OperationalProfileResolver operationalProfileResolver() {
        return OperationalProfileResolver.standard();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperationalGovernancePolicyEnforcer operationalGovernancePolicyEnforcer(
            PlatformGovernanceProperties properties,
            PolicyConfigSource policyConfigSource,
            org.springframework.beans.factory.ObjectProvider<AuditSink> auditSinks,
            org.springframework.beans.factory.ObjectProvider<AuditContextResolver> contextResolvers,
            OperationalProfileResolver operationalProfileResolver,
            Environment environment
    ) {
        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                policyConfigSource,
                operationalProfileResolver,
                environment.getActiveProfiles(),
                auditSinks.orderedStream().toList().size(),
                contextResolvers.orderedStream().toList().size()
        );
        enforcer.enforce();
        return enforcer;
    }

    @Bean
    @ConditionalOnMissingBean
    public FeatureFlagClient platformGovernanceFeatureFlagClient(PlatformGovernanceProperties properties) {
        PlatformGovernanceProperties.FeatureFlags engine = properties.effectiveFeatureFlags();
        FeatureFlagConfig.Builder builder = FeatureFlagConfig.builder()
                .store(FeatureFlagConfig.Store.valueOf(engine.getStore().name()))
                .cacheTtl(Duration.ofMillis(engine.getCacheTtlMillis()));
        if (engine.getFilePath() != null && !engine.getFilePath().isBlank()) {
            builder.filePath(engine.getFilePath());
        }
        return FeatureFlagClientFactory.create(builder.build());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditContextResolver platformGovernanceMdcAuditContextResolver() {
        return new MdcAuditContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogger platformGovernanceAuditLogger(
            PlatformGovernanceProperties properties,
            org.springframework.beans.factory.ObjectProvider<AuditSink> auditSinks,
            org.springframework.beans.factory.ObjectProvider<AuditContextResolver> contextResolvers,
            org.springframework.beans.factory.ObjectProvider<AuditMaskingPolicy> maskingPolicy
    ) {
        List<AuditSink> sinks = auditSinks.orderedStream().toList();
        AuditSink sink = sinks.isEmpty() ? defaultAuditSink(properties) : new CompositeAuditSink(sinks);
        return new DefaultAuditLogger(sink, contextResolvers.orderedStream().toList(), maskingPolicy.getIfAvailable());
    }

    @Bean(name = "platformGovernanceCoreAuditLogRecorder")
    @ConditionalOnMissingBean(name = "platformGovernanceCoreAuditLogRecorder")
    public AuditLogRecorder platformGovernanceCoreAuditLogRecorder(
            PlatformGovernanceProperties properties,
            AuditLogger auditLogger
    ) {
        if (!properties.getAudit().isEnabled()) {
            return entry -> { };
        }
        return new SanitizingAuditLogRecorder(new AuditLoggerAuditLogRecorder(auditLogger), serviceAuditAttributes(properties));
    }

    @Bean(name = "platformGovernanceAuditLogRecorder")
    @Primary
    public AuditLogRecorder auditLogRecorder(
            PlatformGovernanceProperties properties,
            @Qualifier("platformGovernanceCoreAuditLogRecorder") AuditLogRecorder platformRecorder,
            org.springframework.beans.factory.ObjectProvider<AuditLogRecorder> auditLogRecorders
    ) {
        List<AuditLogRecorder> externalRecorders = auditLogRecorders.orderedStream()
                .filter(recorder -> recorder != platformRecorder)
                .toList();
        if (externalRecorders.isEmpty()) {
            return platformRecorder;
        }
        if (!properties.getCompat().isAuditLogRecorderFanoutEnabled()) {
            LOGGER.warn("External AuditLogRecorder bean detected, but compatibility fan-out is disabled. "
                    + "Ignoring external recorders. Migrate to AuditSink or set "
                    + "platform.governance.compat.audit-log-recorder-fanout-enabled=true temporarily.");
            return platformRecorder;
        }
        LOGGER.warn("External AuditLogRecorder bean detected. This compatibility fan-out path is deprecated "
                + "since 2.0.1. Use AuditSink instead. Removal planned for 3.0.0.");
        List<AuditLogRecorder> recorders = new java.util.ArrayList<>();
        recorders.add(platformRecorder);
        recorders.addAll(externalRecorders);
        return new CompositeAuditLogRecorder(recorders);
    }

    @Bean
    @ConditionalOnMissingBean(IdentityAuditRecorder.class)
    public IdentityAuditRecorder identityAuditRecorder(
            PlatformGovernanceProperties properties,
            AuditLogger auditLogger,
            org.springframework.beans.factory.ObjectProvider<IdentityAuditCustomizer> customizers,
            org.springframework.beans.factory.ObjectProvider<AuditAttributeEnricher> enrichers,
            org.springframework.beans.factory.ObjectProvider<AuditContextResolver> contextResolvers
    ) {
        if (!properties.getAudit().isEnabled() || !properties.getAudit().getIdentity().isEnabled()) {
            return event -> { };
        }
        List<IdentityAuditCustomizer> identityCustomizers = new java.util.ArrayList<>();
        identityCustomizers.add(serviceIdentityCustomizer(properties));
        identityCustomizers.addAll(customizers.orderedStream().toList());
        return new DefaultIdentityAuditRecorder(
                auditLogger,
                new IdentityAuditEventValidator(),
                properties.getAudit().getIdentity().isValidationEnabled(),
                properties.getAudit().getIdentity().isFailOnValidationError(),
                properties.getAudit().getFailurePolicy(),
                identityCustomizers,
                enrichers.orderedStream().toList(),
                contextResolvers.orderedStream().toList()
        );
    }

    @Bean
    @ConditionalOnMissingBean(PolicyChangeRecorder.class)
    public PolicyChangeRecorder policyChangeRecorder(AuditLogRecorder auditLogRecorder) {
        return new AuditPolicyChangeRecorder(auditLogRecorder);
    }

    @Bean
    @ConditionalOnMissingBean(name = "platformGovernanceAuditViolationHandler")
    public ViolationHandler platformGovernanceAuditViolationHandler(AuditLogRecorder auditLogRecorder) {
        return new AuditViolationHandler(auditLogRecorder);
    }

    @Bean
    @ConditionalOnMissingBean(GovernanceDecisionEngine.class)
    public GovernanceDecisionEngine governanceDecisionEngine(
            List<GovernancePolicyPlugin> plugins,
            PlatformGovernanceProperties properties
    ) {
        return new PluginGovernanceDecisionEngine(
                plugins,
                properties.getEngine().isStrict(),
                properties.getEngine().getFailurePolicy()
        );
    }

    @Bean(name = "platformGovernancePolicyService")
    @Primary
    public GovernancePolicyService governancePolicyService(
            GovernanceDecisionEngine delegate,
            PlatformGovernanceProperties properties,
            AuditLogRecorder auditLogRecorder,
            List<ViolationHandler> violationHandlers
    ) {
        return (request, context) -> {
            GovernanceVerdict verdict = delegate.evaluate(request, context);
            Map<String, String> attributes = governanceAuditAttributes(request, context, verdict, properties);
            auditLogRecorder.record(new AuditEntry(
                    "governance",
                    "policy evaluated",
                    attributes,
                    request.occurredAt()
            ));
            if (!verdict.allowed()) {
                GovernanceViolation violation = new GovernanceViolation(
                        request,
                        context,
                        verdict,
                        properties.getViolation().getAction(),
                        attributes,
                        request.occurredAt()
                );
                handleViolation(violation, violationHandlers, auditLogRecorder, properties.getViolation().isHandlerFailureFatal());
            }
            return verdict;
        };
    }

    private static Map<String, String> governanceAuditAttributes(
            GovernanceRequest request,
            GovernanceContext context,
            GovernanceVerdict verdict,
            PlatformGovernanceProperties properties
    ) {
        Map<String, String> attributes = new LinkedHashMap<>(serviceAuditAttributes(properties));
        putIfPresent(attributes, "request.subject", request.subject());
        putIfPresent(attributes, "request.resource", request.resource());
        putIfPresent(attributes, "request.action", request.action());
        putIfPresent(attributes, "context.actor", context.actor());
        putIfPresent(attributes, "context.environment", context.environment());
        attributes.put("governance.decision", verdict.decision().name());
        attributes.put("policy.id", verdict.policy());
        putIfPresent(attributes, "policy.reason", verdict.reason());
        putPolicyEvidence(attributes, verdict.evidence());
        putIfPresent(attributes, "engine.failure-type", failureType(verdict));
        return attributes;
    }

    private static void putPolicyEvidence(Map<String, String> attributes, PolicyEvidence evidence) {
        if (evidence == null) {
            return;
        }
        putIfPresent(attributes, "policy.evidence.id", evidence.policyId());
        putIfPresent(attributes, "policy.evidence.version", evidence.policyVersion());
        putIfPresent(attributes, "policy.evidence.decision", evidence.decision());
        putIfPresent(attributes, "policy.evidence.rule-id", evidence.ruleId());
        putIfPresent(attributes, "policy.evidence.violation-code", evidence.violationCode());
    }

    private static String failureType(GovernanceVerdict verdict) {
        if (verdict.allowed() || verdict.reason() == null) {
            return null;
        }
        if ("no matching plugin".equals(verdict.reason())) {
            return "no-matching-plugin";
        }
        if (verdict.reason().startsWith("plugin threw exception:")) {
            return "plugin-exception";
        }
        return "plugin-deny";
    }

    private static void handleViolation(
            GovernanceViolation violation,
            List<ViolationHandler> violationHandlers,
            AuditLogRecorder auditLogRecorder,
            boolean handlerFailureFatal
    ) {
        for (ViolationHandler handler : violationHandlers) {
            try {
                handler.handle(violation);
            } catch (RuntimeException exception) {
                auditLogRecorder.record(new AuditEntry(
                        "governance-violation",
                        "violation handler failed",
                        Map.of(
                                "violation.action", violation.action().name(),
                                "violation.policy", violation.verdict().policy(),
                                "handler", handler.getClass().getName(),
                                "error", exception.getClass().getName()
                        ),
                        violation.occurredAt()
                ));
                if (handlerFailureFatal) {
                    throw exception;
                }
            }
        }
    }

    private static IdentityAuditCustomizer serviceIdentityCustomizer(PlatformGovernanceProperties properties) {
        return event -> event.toBuilder()
                .correlation(event.correlation().mergeMissing(AuditCorrelation.builder()
                        .serviceName(properties.getAudit().getServiceName())
                        .environment(properties.getAudit().getEnvironment())
                .build()))
                .build();
    }

    private static AuditSink defaultAuditSink(PlatformGovernanceProperties properties) {
        if (properties.getAudit().isEnabled()) {
            LOGGER.warn("No AuditSink bean detected. Falling back to LoggingAuditSink so governance audit events are "
                    + "visible in application logs. Register an AuditSink bean for durable delivery.");
        }
        return new LoggingAuditSink();
    }

    private static Map<String, String> serviceAuditAttributes(PlatformGovernanceProperties properties) {
        Map<String, String> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "audit.service-name", properties.getAudit().getServiceName());
        putIfPresent(attributes, "audit.environment", properties.getAudit().getEnvironment());
        return attributes;
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }
}
