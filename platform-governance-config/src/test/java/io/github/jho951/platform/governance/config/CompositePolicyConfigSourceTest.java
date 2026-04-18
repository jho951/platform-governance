package io.github.jho951.platform.governance.config;

import io.github.jho951.platform.governance.api.PolicyConfigSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositePolicyConfigSourceTest {
    @Test
    void isOperationalRequiresAtLeastOneOperationalSource() {
        assertFalse(new CompositePolicyConfigSource(java.util.List.of()).isOperational());
        assertFalse(new CompositePolicyConfigSource(java.util.List.of(new NonOperationalSource())).isOperational());
        assertTrue(new CompositePolicyConfigSource(java.util.List.of(new MapPolicyConfigSource(Map.of("feature", "true")))).isOperational());
    }

    @Test
    void supportsSnapshotOnlyWhenEverySourceSupportsSnapshot() {
        CompositePolicyConfigSource source = new CompositePolicyConfigSource(java.util.List.of(
                new MapPolicyConfigSource(Map.of("feature", "true")),
                new SnapshotlessOperationalSource()
        ));

        assertFalse(source.supportsSnapshot());
        assertThrows(IllegalStateException.class, source::snapshot);
    }

    @Test
    void snapshotMergesSupportedSourcesWithoutDroppingValues() {
        CompositePolicyConfigSource source = new CompositePolicyConfigSource(java.util.List.of(
                new MapPolicyConfigSource(Map.of("feature", "true")),
                new MapPolicyConfigSource(Map.of("limit", "10"))
        ));

        assertEquals(Map.of("feature", "true", "limit", "10"), source.snapshot());
    }

    private static final class NonOperationalSource implements PolicyConfigSource {
        @Override
        public Optional<String> resolve(String key) {
            return Optional.empty();
        }

        @Override
        public Map<String, String> snapshot() {
            return Map.of();
        }
    }

    private static final class SnapshotlessOperationalSource implements PolicyConfigSource {
        @Override
        public Optional<String> resolve(String key) {
            return Optional.of("true");
        }

        @Override
        public Map<String, String> snapshot() {
            throw new IllegalStateException("snapshot unsupported");
        }

        @Override
        public boolean supportsSnapshot() {
            return false;
        }

        @Override
        public boolean isOperational() {
            return true;
        }
    }
}
