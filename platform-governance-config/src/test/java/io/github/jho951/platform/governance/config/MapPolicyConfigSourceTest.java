package io.github.jho951.platform.governance.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapPolicyConfigSourceTest {
    @Test
    void resolvesValueFromMap() {
        MapPolicyConfigSource source = new MapPolicyConfigSource(Map.of("feature.review.required", "true"));

        assertEquals("true", source.resolve("feature.review.required").orElseThrow());
    }

    @Test
    void emptyMapSourceIsNotOperational() {
        MapPolicyConfigSource source = new MapPolicyConfigSource(Map.of());

        assertEquals(false, source.operationalStatus().isOperational());
        assertEquals("NOT_CONFIGURED", source.operationalStatus().state().name());
    }
}
