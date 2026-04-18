package io.github.jho951.platform.governance.api;

import java.util.Map;
import java.util.Optional;

public interface PolicyConfigSource {
    Optional<String> resolve(String key);

    Map<String, String> snapshot();

    default boolean supportsSnapshot() {
        return true;
    }

    /**
     * Returns whether this source is safe to use for enforcing policy decisions.
     */
    default boolean isOperational() {
        return false;
    }
}
