package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SanitizingGovernanceAuditRecorder implements GovernanceAuditRecorder {
    private final GovernanceAuditRecorder delegate;
    private final Map<String, String> baseAttributes;

    public SanitizingGovernanceAuditRecorder(GovernanceAuditRecorder delegate, Map<String, String> baseAttributes) {
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
