package io.github.jho951.platform.governance.samples;

import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditSink;
import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import io.github.jho951.platform.governance.api.PolicyConfigOperationalStatus;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformGovernanceSamplesSmokeTest {
    private final CapturingAuditSink capturingAuditSink = new CapturingAuditSink();
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(resolveClass(
                    "io.github.jho951.platform.governance.spring.PlatformGovernanceAutoConfiguration"
            )))
            .withBean(AuditSink.class, () -> capturingAuditSink)
            .withBean(PolicyConfigSource.class, () -> new FixturePolicyConfigSource(Map.of(
                    "feature.review.required", "true"
            )))
            .withBean(GovernancePolicyPlugin.class, FixtureGovernancePolicyPlugin::new);

    @Test
    void startsInDevProfile() {
        runner
                .withPropertyValues(
                        "spring.profiles.active=dev",
                        "platform.governance.audit.service-name=platform-governance-samples",
                        "platform.governance.audit.environment=dev"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(resolveClass(
                            "io.github.jho951.platform.governance.spring.PlatformGovernanceProperties"
                    ));
                });
    }

    @Test
    void startsInProdProfileWithRequiredRuntimeContracts() {
        runner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.governance.engine.strict=true",
                        "platform.governance.audit.service-name=platform-governance-samples",
                        "platform.governance.audit.environment=prod",
                        "platform.governance.violation.handler-failure-fatal=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(resolveClass(
                            "io.github.jho951.platform.governance.spring.PlatformGovernanceProperties"
                    ));
                });
    }

    @Test
    void officialAuditSinkSurfaceRecordsGovernanceViolations() {
        capturingAuditSink.clear();

        runner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.governance.audit.service-name=platform-governance-samples",
                        "platform.governance.audit.environment=prod",
                        "platform.governance.engine.strict=true",
                        "platform.governance.violation.handler-failure-fatal=true"
                )
                .run(context -> {
                    GovernancePolicyService policyService = context.getBean(GovernancePolicyService.class);

                    GovernanceVerdict verdict = policyService.evaluate(
                            new GovernanceRequest(
                                    "user-1",
                                    "/documents/1",
                                    "review",
                                    Map.of("feature.review.required", "true"),
                                    java.time.Instant.parse("2026-01-01T00:00:00Z")
                            ),
                            new GovernanceContext("actor-1", "prod", Map.of("tenant", "sample"))
                    );

                    assertThat(verdict.decision().name()).isEqualTo("DENY");
                    assertThat(capturingAuditSink.events())
                            .isNotEmpty()
                            .anySatisfy(event -> assertThat(event.getAction()).isEqualTo("governance violation handled"));
                });
    }

    @Test
    void externalAuditLogRecorderIsIgnoredByMainlineStarter() {
        capturingAuditSink.clear();
        AtomicInteger externalRecorderCalls = new AtomicInteger();

        runner
                .withBean((Class<Object>) resolveClass("io.github.jho951.platform.governance.api.AuditLogRecorder"),
                        () -> java.lang.reflect.Proxy.newProxyInstance(
                                PlatformGovernanceSamplesSmokeTest.class.getClassLoader(),
                                new Class<?>[]{resolveClass("io.github.jho951.platform.governance.api.AuditLogRecorder")},
                                (proxy, method, args) -> {
                                    if ("record".equals(method.getName())) {
                                        externalRecorderCalls.incrementAndGet();
                                    }
                                    return null;
                                }))
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "platform.governance.audit.service-name=platform-governance-samples",
                        "platform.governance.audit.environment=prod",
                        "platform.governance.engine.strict=true",
                        "platform.governance.violation.handler-failure-fatal=true"
                )
                .run(context -> {
                    GovernancePolicyService policyService = context.getBean(GovernancePolicyService.class);

                    policyService.evaluate(
                            new GovernanceRequest(
                                    "user-1",
                                    "/documents/1",
                                    "review",
                                    Map.of("feature.review.required", "true"),
                                    java.time.Instant.parse("2026-01-01T00:00:00Z")
                            ),
                            new GovernanceContext("actor-1", "prod", Map.of())
                    );

                    assertThat(externalRecorderCalls.get()).isZero();
                    assertThat(capturingAuditSink.events()).isNotEmpty();
                });
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Runtime class is not available: " + className, exception);
        }
    }

    private record FixturePolicyConfigSource(Map<String, String> values) implements PolicyConfigSource {
        @Override
        public Optional<String> resolve(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public Map<String, String> snapshot() {
            return values;
        }

        @Override
        public PolicyConfigOperationalStatus operationalStatus() {
            if (values.isEmpty()) {
                return PolicyConfigOperationalStatus.notConfigured("fixture policy config is empty");
            }
            return PolicyConfigOperationalStatus.operational("fixture policy config has values");
        }
    }

    private static final class FixtureGovernancePolicyPlugin implements GovernancePolicyPlugin {
        @Override
        public String name() {
            return "fixture-policy";
        }

        @Override
        public boolean supports(GovernanceRequest request, GovernanceContext context) {
            return true;
        }

        @Override
        public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
            if ("true".equalsIgnoreCase(request.attributes().get("feature.review.required"))) {
                return GovernanceVerdict.deny(name(), "review is required");
            }
            return GovernanceVerdict.allow(name(), "fixture policy matched");
        }
    }

    private static final class CapturingAuditSink implements AuditSink {
        private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void write(AuditEvent event) {
            events.add(event);
        }

        java.util.List<AuditEvent> events() {
            return java.util.List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
