package io.github.jho951.platform.governance.spring;

import com.auditlog.api.AuditLogger;
import com.pluginpolicyengine.api.FeatureFlagClient;
import com.policyconfig.contracts.PolicyKey;
import com.policyconfig.contracts.PolicyResolver;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.GovernanceContext;
import io.github.jho951.platform.governance.api.GovernanceDecision;
import io.github.jho951.platform.governance.api.GovernancePolicyPlugin;
import io.github.jho951.platform.governance.api.GovernancePolicyService;
import io.github.jho951.platform.governance.api.GovernanceRequest;
import io.github.jho951.platform.governance.api.GovernanceVerdict;
import io.github.jho951.platform.governance.api.PolicyConfigSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlatformGovernanceAutoConfigurationTest {
    private final PlatformGovernanceAutoConfiguration configuration = new PlatformGovernanceAutoConfiguration();

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
        assertNotNull(configuration.auditLogRecorder(new PlatformGovernanceProperties(), provider(auditLogger)));
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

        GovernancePolicyService service = configuration.governancePolicyService(java.util.List.of(plugin), properties);
        GovernanceVerdict verdict = service.evaluate(
                new GovernanceRequest("user-1", "/resource", "review", Map.of(), Instant.parse("2026-01-01T00:00:00Z")),
                new GovernanceContext("actor-1", "prod", Map.of())
        );

        assertEquals(GovernanceDecision.DENY, verdict.decision());
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
}
