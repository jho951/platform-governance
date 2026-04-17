package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.PolicyChangeEvent;
import io.github.jho951.platform.governance.api.PolicyChangeRecorder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AuditPolicyChangeRecorder implements PolicyChangeRecorder {
    private final AuditLogRecorder auditLogRecorder;

    public AuditPolicyChangeRecorder(AuditLogRecorder auditLogRecorder) {
        this.auditLogRecorder = Objects.requireNonNull(auditLogRecorder, "auditLogRecorder");
    }

    @Override
    public void record(PolicyChangeEvent event) {
        Objects.requireNonNull(event, "event");

        Map<String, String> attributes = new LinkedHashMap<>(event.attributes());
        attributes.put("policy.key", event.policyKey());
        putPolicyValue(attributes, "policy.previous-value", event);
        putPolicyValue(attributes, "policy.new-value", event);
        putIfPresent(attributes, "policy.actor", event.actor());
        putIfPresent(attributes, "policy.reason", event.reason());

        auditLogRecorder.record(new AuditEntry(
                "policy-change",
                "policy changed",
                SensitiveAuditRedactor.redactStrings(attributes),
                event.occurredAt()
        ));
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }

    private static void putPolicyValue(Map<String, String> attributes, String attributeKey, PolicyChangeEvent event) {
        String value = "policy.previous-value".equals(attributeKey) ? event.previousValue() : event.newValue();
        if (value == null) {
            return;
        }
        attributes.put(attributeKey, SensitiveAuditRedactor.redactValue(event.policyKey(), value));
    }
}
