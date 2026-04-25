package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditLogger;
import com.auditlog.api.AuditResult;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditRecorder;

import java.util.Objects;

public final class AuditLoggerGovernanceAuditRecorder implements GovernanceAuditRecorder {
    private final AuditLogger auditLogger;

    public AuditLoggerGovernanceAuditRecorder(AuditLogger auditLogger) {
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
    }

    @Override
    public void record(AuditEntry entry) {
        auditLogger.log(AuditEvent.builder(AuditEventType.SYSTEM, entry.message())
                .actor("platform-governance", AuditActorType.SYSTEM, "platform-governance")
                .resource(entry.category(), entry.category())
                .result(AuditResult.SUCCESS)
                .details(SensitiveAuditRedactor.redactStrings(entry.attributes()))
                .occurredAt(entry.occurredAt())
                .build());
    }
}
