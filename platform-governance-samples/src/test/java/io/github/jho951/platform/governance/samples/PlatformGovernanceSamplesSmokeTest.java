package io.github.jho951.platform.governance.samples;

import com.auditlog.api.AuditSink;
import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import io.github.jho951.platform.governance.spring.PlatformGovernanceAutoConfiguration;
import io.github.jho951.platform.governance.spring.PlatformGovernanceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformGovernanceSamplesSmokeTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformGovernanceAutoConfiguration.class))
            .withBean(AuditSink.class, () -> event -> { })
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
                    assertThat(context).hasSingleBean(PlatformGovernanceProperties.class);
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
                    assertThat(context).hasSingleBean(PlatformGovernanceProperties.class);
                });
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
        public boolean isOperational() {
            return !values.isEmpty();
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
            return GovernanceVerdict.allow(name(), "fixture policy matched");
        }
    }
}
