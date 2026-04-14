package io.github.jho951.platform.governance.api;

import java.util.Map;
import java.util.Optional;

public interface PolicyConfigSource {
    Optional<String> resolve(String key);

    Map<String, String> snapshot();
}

