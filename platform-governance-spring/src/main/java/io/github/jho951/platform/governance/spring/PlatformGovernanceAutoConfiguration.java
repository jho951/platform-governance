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
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.audit.AuditLoggerAuditLogRecorder;
import io.github.jho951.platform.governance.audit.InMemoryAuditLogRecorder;
import io.github.jho951.platform.governance.config.PolicyResolverPolicyConfigSource;
import io.github.jho951.platform.governance.engine.PluginGovernancePolicyEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.governance", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PlatformGovernanceProperties.class)
public class PlatformGovernanceAutoConfiguration {
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
    public FeatureFlagClient platformGovernanceFeatureFlagClient(PlatformGovernanceProperties properties) {
        PlatformGovernanceProperties.PluginPolicyEngine engine = properties.getPluginPolicyEngine();
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
    public AuditLogger platformGovernanceAuditLogger(
            org.springframework.beans.factory.ObjectProvider<AuditSink> auditSinks,
            org.springframework.beans.factory.ObjectProvider<AuditContextResolver> contextResolvers,
            org.springframework.beans.factory.ObjectProvider<AuditMaskingPolicy> maskingPolicy
    ) {
        List<AuditSink> sinks = auditSinks.orderedStream().toList();
        AuditSink sink = sinks.isEmpty() ? event -> { } : new CompositeAuditSink(sinks);
        return new DefaultAuditLogger(sink, contextResolvers.orderedStream().toList(), maskingPolicy.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(AuditLogRecorder.class)
    public AuditLogRecorder auditLogRecorder(PlatformGovernanceProperties properties, org.springframework.beans.factory.ObjectProvider<AuditLogger> auditLogger) {
        if (!properties.getAudit().isEnabled()) {
            return entry -> { };
        }
        AuditLogger logger = auditLogger.getIfAvailable();
        if (logger != null) {
            return new AuditLoggerAuditLogRecorder(logger);
        }
        return new InMemoryAuditLogRecorder();
    }

    @Bean
    @Order(1)
    @ConditionalOnMissingBean(name = "auditEnvelope")
    public GovernancePolicyPlugin auditEnvelope(PolicyConfigSource policyConfigSource, AuditLogRecorder auditLogRecorder) {
        return new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "audit-envelope";
            }

            @Override
            public boolean supports(io.github.jho951.platform.governance.api.GovernanceRequest request, io.github.jho951.platform.governance.api.GovernanceContext context) {
                return true;
            }

            @Override
            public io.github.jho951.platform.governance.api.GovernanceVerdict evaluate(
                    io.github.jho951.platform.governance.api.GovernanceRequest request,
                    io.github.jho951.platform.governance.api.GovernanceContext context
            ) {
                auditLogRecorder.record(new AuditEntry(
                        "governance",
                        "policy evaluated",
                        policyConfigSource.snapshot(),
                        request.occurredAt()
                ));
                return io.github.jho951.platform.governance.api.GovernanceVerdict.allow(name(), "audited");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(GovernancePolicyService.class)
    public GovernancePolicyService governancePolicyService(List<GovernancePolicyPlugin> plugins, PlatformGovernanceProperties properties) {
        return new PluginGovernancePolicyEngine(plugins, properties.getEngine().isStrict());
    }
}
