package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;

import java.util.List;
import java.util.Objects;

public final class CompositeGovernanceAuditRecorder implements GovernanceAuditRecorder {
    private final List<GovernanceAuditRecorder> recorders;

    public CompositeGovernanceAuditRecorder(List<GovernanceAuditRecorder> recorders) {
        this.recorders = recorders == null ? List.of() : recorders.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void record(AuditEntry entry) {
        for (GovernanceAuditRecorder recorder : recorders) {
            recorder.record(entry);
        }
    }
}
