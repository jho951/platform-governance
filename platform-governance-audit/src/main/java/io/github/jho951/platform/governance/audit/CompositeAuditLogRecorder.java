package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;

import java.util.List;
import java.util.Objects;

public final class CompositeAuditLogRecorder implements AuditLogRecorder {
    private final List<AuditLogRecorder> recorders;

    public CompositeAuditLogRecorder(List<AuditLogRecorder> recorders) {
        this.recorders = recorders == null ? List.of() : recorders.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void record(AuditEntry entry) {
        for (AuditLogRecorder recorder : recorders) {
            recorder.record(entry);
        }
    }
}
