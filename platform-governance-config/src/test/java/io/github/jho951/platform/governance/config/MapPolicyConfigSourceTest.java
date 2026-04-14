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
}
