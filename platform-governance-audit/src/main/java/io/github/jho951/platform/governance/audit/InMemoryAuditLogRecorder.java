package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditLogRecorder implements AuditLogRecorder {
    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public synchronized void record(AuditEntry entry) {
        entries.add(entry);
    }

    public synchronized List<AuditEntry> entries() {
        return List.copyOf(entries);
    }
}
