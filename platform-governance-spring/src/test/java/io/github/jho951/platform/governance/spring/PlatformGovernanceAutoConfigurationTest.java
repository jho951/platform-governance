package io.github.jho951.platform.governance.spring;

import com.auditlog.api.AuditLogger;
import com.auditlog.api.AuditSink;
import com.pluginpolicyengine.api.FeatureFlagClient;
import com.policyconfig.contracts.PolicyKey;
import com.policyconfig.contracts.PolicyResolver;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceDecisionEngine;
import io.github.jho951.platform.governance.api.GovernanceViolation;
import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecision;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import io.github.jho951.platform.governance.api.PolicyChangeEvent;
import io.github.jho951.platform.governance.api.PolicyChangeRecorder;
import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.api.ViolationAction;
import io.github.jho951.platform.governance.api.ViolationHandler;
import io.github.jho951.platform.governance.identity.IdentityAuditRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformGovernanceAutoConfigurationTest {
    private final PlatformGovernanceAutoConfiguration configuration = new PlatformGovernanceAutoConfiguration();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformGovernanceAutoConfiguration.class));

    @Test
    void policyConfigSourceUsesPropertyValues() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getPolicyConfig().getValues().put("feature.review.required", "true");
        PolicyResolver resolver = configuration.platformGovernancePolicyResolver(properties);

        PolicyConfigSource source = configuration.policyConfigSource(resolver);
        assertEquals("true", source.resolve("feature.review.required").orElseThrow());
    }

    @Test
    void policyResolverUsesPropertyValues() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getPolicyConfig().getValues().put("feature.review.required", "true");

        PolicyResolver resolver = configuration.platformGovernancePolicyResolver(properties);

        assertEquals(true, resolver.get(PolicyKey.builder("feature.review.required", Boolean.class).build()));
    }

    @Test
    void auditRecorderExists() {
        AuditLogger auditLogger = configuration.platformGovernanceAuditLogger(emptyProvider(), emptyProvider(), emptyProvider());
        AuditLogRecorder coreRecorder = configuration.platformGovernanceCoreAuditLogRecorder(
                new PlatformGovernanceProperties(),
                provider(auditLogger)
        );

        assertNotNull(configuration.auditLogRecorder(coreRecorder, provider(coreRecorder)));
    }

    @Test
    void auditRecorderFansOutToUserRecorders() {
        List<String> categories = new ArrayList<>();
        AuditLogRecorder coreRecorder = entry -> categories.add("platform:" + entry.category());
        AuditLogRecorder userRecorder = entry -> categories.add("user:" + entry.category());
        AuditLogRecorder recorder = configuration.auditLogRecorder(coreRecorder, provider(userRecorder));

        recorder.record(new AuditEntry(
                "governance",
                "policy evaluated",
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        assertEquals(List.of("platform:governance", "user:governance"), categories);
    }

    @Test
    void identityAuditRecorderExists() {
        AuditLogger auditLogger = configuration.platformGovernanceAuditLogger(emptyProvider(), emptyProvider(), emptyProvider());

        IdentityAuditRecorder recorder = configuration.identityAuditRecorder(
                new PlatformGovernanceProperties(),
                auditLogger,
                emptyProvider(),
                emptyProvider(),
                emptyProvider()
        );

        assertNotNull(recorder);
    }

    @Test
    void featureFlagClientExists() {
        FeatureFlagClient client = configuration.platformGovernanceFeatureFlagClient(new PlatformGovernanceProperties());

        assertNotNull(client);
    }

    @Test
    void governanceEngineEvaluatesPlugin() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        GovernancePolicyPlugin plugin = new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "deny-review";
            }

            @Override
            public boolean supports(GovernanceRequest request, GovernanceContext context) {
                return true;
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                return GovernanceVerdict.deny(name(), "blocked");
            }
        };

        GovernancePolicyService service = configuration.governancePolicyService(
                configuration.governanceDecisionEngine(java.util.List.of(plugin), properties),
                properties,
                entry -> { },
                java.util.List.of()
        );
        GovernanceVerdict verdict = service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
    }

    @Test
    void strictEngineDeniesWhenNoPolicyPluginMatches() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getEngine().setStrict(true);

        GovernancePolicyService service = configuration.governancePolicyService(
                configuration.governanceDecisionEngine(java.util.List.of(), properties),
                properties,
                entry -> { },
                java.util.List.of()
        );

        GovernanceVerdict verdict = service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
        assertEquals("no matching plugin", verdict.reason());
    }

    @Test
    void defaultPolicyChangeRecorderWritesAuditEntry() {
        List<String> categories = new ArrayList<>();
        AuditLogRecorder auditLogRecorder = entry -> categories.add(entry.category());
        PolicyChangeRecorder recorder = configuration.policyChangeRecorder(auditLogRecorder);

        recorder.record(new PolicyChangeEvent(
                "operator-1",
                "feature.review.required",
                "false",
                "true",
                "enable review gate",
                Map.of(),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        assertEquals(List.of("policy-change"), categories);
    }

    @Test
    void deniedVerdictRunsViolationHandlers() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        GovernancePolicyPlugin plugin = new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "deny-review";
            }

            @Override
            public boolean supports(GovernanceRequest request, GovernanceContext context) {
                return true;
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                return GovernanceVerdict.deny(name(), "blocked");
            }
        };
        List<GovernanceViolation> violations = new ArrayList<>();
        ViolationHandler handler = violations::add;

        GovernancePolicyService service = configuration.governancePolicyService(
                configuration.governanceDecisionEngine(java.util.List.of(plugin), properties),
                properties,
                entry -> { },
                java.util.List.of(handler)
        );

        service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(1, violations.size());
        assertEquals("deny-review", violations.get(0).verdict().policy());
    }

    @Test
    void violationActionDoesNotRewritePolicyVerdict() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getViolation().setAction(ViolationAction.AUDIT_ONLY);
        GovernancePolicyPlugin plugin = new GovernancePolicyPlugin() {
            @Override
            public String name() {
                return "deny-review";
            }

            @Override
            public boolean supports(GovernanceRequest request, GovernanceContext context) {
                return true;
            }

            @Override
            public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
                return GovernanceVerdict.deny(name(), "blocked");
            }
        };

        GovernancePolicyService service = configuration.governancePolicyService(
                configuration.governanceDecisionEngine(java.util.List.of(plugin), properties),
                properties,
                entry -> { },
                java.util.List.of()
        );

        GovernanceVerdict verdict = service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
    }

    @Test
    void governanceAuditUsesDecisionEvidenceInsteadOfPolicySnapshot() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getAudit().setServiceName("auth-service");
        properties.getAudit().setEnvironment("prod");
        GovernanceDecisionEngine decisionEngine = (request, context) -> GovernanceVerdict.allow("review-policy", "matched");
        List<AuditEntry> entries = new ArrayList<>();

        GovernancePolicyService service = configuration.governancePolicyService(
                decisionEngine,
                properties,
                entries::add,
                java.util.List.of()
        );

        service.evaluate(
                new GovernanceRequest("user-1", "/documents/1", "read", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        Map<String, String> attributes = entries.get(0).attributes();
        assertEquals("user-1", attributes.get("request.subject"));
        assertEquals("/documents/1", attributes.get("request.resource"));
        assertEquals("read", attributes.get("request.action"));
        assertEquals("actor-1", attributes.get("context.actor"));
        assertEquals("auth-service", attributes.get("audit.service-name"));
        assertEquals("prod", attributes.get("audit.environment"));
        assertEquals(null, attributes.get("feature.review.required"));
    }

    @Test
    void operationalEnforcerRequiresFilePathForFileStore() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getFeatureFlags().setStore(PlatformGovernanceProperties.FeatureFlags.Store.FILE);
        properties.getOperational().setFailFastEnabled(false);
        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                emptyPolicyConfigSource(),
                new String[0],
                0
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, enforcer::enforce);

        assertTrue(exception.getMessage().contains("feature-flags.file-path"));
    }

    @Test
    void operationalEnforcerRejectsUnsafeProductionDefaults() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                emptyPolicyConfigSource(),
                new String[] {"prod"},
                0
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, enforcer::enforce);

        assertTrue(exception.getMessage().contains("engine.strict=false"));
        assertTrue(exception.getMessage().contains("AuditSink"));
        assertTrue(exception.getMessage().contains("AuditContextResolver"));
        assertTrue(exception.getMessage().contains("audit.service-name"));
        assertTrue(exception.getMessage().contains("policy config source must be operational"));
        assertTrue(exception.getMessage().contains("handler-failure-fatal=false"));
    }

    @Test
    void operationalEnforcerAllowsSnapshotLessOperationalPolicyConfigSource() {
        PlatformGovernanceProperties properties = productionReadyProperties();
        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                snapshotLessPolicyConfigSource(),
                new String[] {"prod"},
                1,
                1
        );

        enforcer.enforce();
    }

    @Test
    void operationalEnforcerAllowsExplicitProductionOverrides() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getOperational().setAllowNonStrictEngineInProduction(true);
        properties.getOperational().setRequireAuditSinkInProduction(false);
        properties.getOperational().setRequireAuditContextResolverInProduction(false);
        properties.getOperational().setRequireAuditServiceIdentityInProduction(false);
        properties.getOperational().setRequirePolicyConfigInEnforcingMode(false);
        properties.getOperational().setRequireFatalHandlerFailuresInProduction(false);
        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                emptyPolicyConfigSource(),
                new String[] {"prod"},
                0
        );

        enforcer.enforce();
    }

    @Test
    void policyDecisionPresetAppliesStrictEnforcingDefaults() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.setServiceRolePreset(GovernanceServiceRolePreset.POLICY_DECISION_SERVICE);

        new PlatformGovernancePresetApplier().apply(properties);

        assertEquals(true, properties.getEngine().isStrict());
        assertEquals(ViolationAction.DENY, properties.getViolation().getAction());
        assertEquals(true, properties.getViolation().isHandlerFailureFatal());
    }

    @Test
    void identityPresetAllowsAuditOnlyProductionShapeWithoutPolicyConfig() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.setServiceRolePreset(GovernanceServiceRolePreset.IDENTITY_SERVICE);
        properties.getAudit().setServiceName("auth-service");
        properties.getAudit().setEnvironment("prod");
        new PlatformGovernancePresetApplier().apply(properties);

        OperationalGovernancePolicyEnforcer enforcer = new OperationalGovernancePolicyEnforcer(
                properties,
                emptyPolicyConfigSource(),
                new String[] {"prod"},
                1,
                1
        );

        enforcer.enforce();
    }

    @Test
    void autoConfigurationFailsWhenFileStoreHasNoPath() {
        contextRunner
                .withPropertyValues(
                        "platform.governance.operational.fail-fast-enabled=false",
                        "platform.governance.feature-flags.store=file"
                )
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("feature-flags.file-path"));
                });
    }

    @Test
    void legacyPluginPolicyEnginePrefixStillBindsAsDeprecatedAlias() {
        contextRunner
                .withPropertyValues(
                        "platform.governance.operational.fail-fast-enabled=false",
                        "platform.governance.plugin-policy-engine.store=file"
                )
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("feature-flags.file-path"));
                });
    }

    @Test
    void autoConfigurationFailsWhenFeatureFlagPrefixesAreMixed() {
        contextRunner
                .withPropertyValues(
                        "platform.governance.operational.fail-fast-enabled=false",
                        "platform.governance.feature-flags.store=memory",
                        "platform.governance.plugin-policy-engine.cache-ttl-millis=5000"
                )
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("Mixed configuration is not supported"));
                });
    }

    @Test
    void autoConfigurationFailsForUnsafeProductionDefaults() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("engine.strict=false"));
                    assertTrue(context.getStartupFailure().getMessage().contains("AuditSink"));
                });
    }

    @Test
    void productionProfileNameIsTreatedAsProductionByDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=production")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("engine.strict=false"));
                });
    }

    @Test
    void autoConfigurationFailsWhenAuditEnvironmentConflictsWithActiveProfile() {
        contextRunner
                .withUserConfiguration(AuditSinkConfiguration.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.governance.engine.strict=true",
                        "platform.governance.audit.service-name=auth-service",
                        "platform.governance.audit.environment=dev",
                        "platform.governance.policy-config.values.feature.review.required=true",
                        "platform.governance.violation.handler-failure-fatal=true"
                )
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("audit.environment=dev conflicts"));
                });
    }

    @Test
    void autoConfigurationStartsWithProductionOverridesAndRequiredBeans() {
        contextRunner
                .withUserConfiguration(AuditSinkConfiguration.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.governance.engine.strict=true",
                        "platform.governance.audit.service-name=auth-service",
                        "platform.governance.audit.environment=prod",
                        "platform.governance.policy-config.values.feature.review.required=true",
                        "platform.governance.violation.handler-failure-fatal=true"
                )
                .run(context -> {
                    assertEquals(null, context.getStartupFailure());
                    assertNotNull(context.getBean(OperationalGovernancePolicyEnforcer.class));
                });
    }

    @Test
    void platformGovernancePolicyServiceRemainsPrimaryWhenUserDefinesSameType() {
        contextRunner
                .withUserConfiguration(CustomGovernancePolicyServiceConfiguration.class)
                .run(context -> {
                    GovernancePolicyService service = context.getBean(GovernancePolicyService.class);
                    CapturingAuditLogRecorder auditLogRecorder = context.getBean(CapturingAuditLogRecorder.class);

                    GovernanceVerdict verdict = service.evaluate(
                            new GovernanceRequest("user-1", "/resource", "read", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                            new GovernanceContext("actor-1", "test", Map.of())
                    );

                    assertEquals(GovernanceDecision.DENY, verdict.decision());
                    assertEquals("user-service", verdict.policy());
                    assertEquals("governance", auditLogRecorder.entries.get(0).category());
                });
    }

    private static PolicyConfigSource emptyPolicyConfigSource() {
        return new PolicyConfigSource() {
            @Override
            public Optional<String> resolve(String key) {
                return Optional.empty();
            }

            @Override
            public Map<String, String> snapshot() {
                return Map.of();
            }
        };
    }

    private static PolicyConfigSource snapshotLessPolicyConfigSource() {
        return new PolicyConfigSource() {
            @Override
            public Optional<String> resolve(String key) {
                return Optional.of("true");
            }

            @Override
            public Map<String, String> snapshot() {
                return Map.of();
            }

        @Override
        public PolicyConfigOperationalStatus operationalStatus() {
            return PolicyConfigOperationalStatus.operational("test source is operational");
        }

        @Override
            public boolean supportsSnapshot() {
                return false;
            }
        };
    }

    private static PlatformGovernanceProperties productionReadyProperties() {
        PlatformGovernanceProperties properties = new PlatformGovernanceProperties();
        properties.getEngine().setStrict(true);
        properties.getAudit().setServiceName("auth-service");
        properties.getAudit().setEnvironment("prod");
        properties.getViolation().setHandlerFailureFatal(true);
        return properties;
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return null;
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public Iterator<T> iterator() {
                return Stream.<T>empty().iterator();
            }

            @Override
            public Stream<T> stream() {
                return Stream.empty();
            }

            @Override
            public Stream<T> orderedStream() {
                return Stream.empty();
            }
        };
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public Iterator<T> iterator() {
                return Stream.of(value).iterator();
            }

            @Override
            public Stream<T> stream() {
                return Stream.of(value);
            }

            @Override
            public Stream<T> orderedStream() {
                return Stream.of(value);
            }
        };
    }

    @Configuration(proxyBeanMethods = false)
    static class AuditSinkConfiguration {
        @Bean
        AuditSink auditSink() {
            return event -> { };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomGovernancePolicyServiceConfiguration {
        @Bean
        CapturingAuditLogRecorder capturingAuditLogRecorder() {
            return new CapturingAuditLogRecorder();
        }

        @Bean
        GovernancePolicyService userGovernancePolicyService() {
            return (request, context) -> GovernanceVerdict.deny("user-service", "custom service should not be primary");
        }
    }

    static class CapturingAuditLogRecorder implements AuditLogRecorder {
        private final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public void record(AuditEntry entry) {
            entries.add(entry);
        }
    }
}
