package io.github.jho951.platform.governance.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalProfileResolverTest {
    @Test
    void standardResolverMatchesConfiguredProductionProfilesOnly() {
        OperationalProfileResolver resolver = OperationalProfileResolver.standard();

        assertTrue(resolver.isProduction(List.of("prod"), List.of("prod")));
        assertFalse(resolver.isProduction(List.of("production"), List.of("prod")));
        assertTrue(resolver.isProduction(List.of("production"), List.of("prod", "production")));
    }
}
