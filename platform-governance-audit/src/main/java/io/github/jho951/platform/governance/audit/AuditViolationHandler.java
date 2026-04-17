package io.github.jho951.platform.governance.audit;

import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.AuditLogRecorder;
import io.github.jho951.platform.governance.api.GovernanceViolation;
import io.github.jho951.platform.governance.api.ViolationHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AuditViolationHandler implements ViolationHandler {
    private final AuditLogRecorder auditLogRecorder;

    public AuditViolationHandler(AuditLogRecorder auditLogRecorder) {
        this.auditLogRecorder = Objects.requireNonNull(auditLogRecorder, "auditLogRecorder");
    }

    @Override
    public void handle(GovernanceViolation violation) {
        Objects.requireNonNull(violation, "violation");

        Map<String, String> attributes = new LinkedHashMap<>(violation.attributes());
        attributes.put("violation.action", violation.action().name());
        attributes.put("violation.policy", violation.verdict().policy());
        putIfPresent(attributes, "violation.reason", violation.verdict().reason());
        putIfPresent(attributes, "request.subject", violation.request().subject());
        attributes.put("request.resource", violation.request().resource());
        attributes.put("request.action", violation.request().action());
        putIfPresent(attributes, "context.actor", violation.context().actor());
        putIfPresent(attributes, "context.environment", violation.context().environment());

        auditLogRecorder.record(new AuditEntry(
                "governance-violation",
                "governance violation handled",
                attributes,
                violation.occurredAt()
        ));
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (value != null) {
            attributes.put(key, value);
        }
    }
}
