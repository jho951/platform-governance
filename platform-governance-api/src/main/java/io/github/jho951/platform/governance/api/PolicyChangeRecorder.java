package io.github.jho951.platform.governance.api;

public interface PolicyChangeRecorder {
    void record(PolicyChangeEvent event);
}
