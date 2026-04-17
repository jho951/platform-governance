package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SanitizingAuditLogRecorder implements AuditLogRecorder {
    private final AuditLogRecorder delegate;
    private final Map<String, String> baseAttributes;

    public SanitizingAuditLogRecorder(AuditLogRecorder delegate, Map<String, String> baseAttributes) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.baseAttributes = baseAttributes == null ? Map.of() : Map.copyOf(baseAttributes);
    }

    @Override
    public void record(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry");
        Map<String, String> attributes = new LinkedHashMap<>(baseAttributes);
        attributes.putAll(entry.attributes());
        delegate.record(new AuditEntry(
                entry.category(),
                entry.message(),
                SensitiveAuditRedactor.redactStrings(attributes),
                entry.occurredAt()
        ));
    }
}
