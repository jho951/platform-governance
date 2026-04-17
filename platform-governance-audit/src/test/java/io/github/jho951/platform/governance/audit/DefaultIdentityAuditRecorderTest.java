package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditContext;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditLogger;
import io.github.jho951.platform.governance.api.AuditActor;
import io.github.jho951.platform.governance.api.AuditFailurePolicy;
import io.github.jho951.platform.governance.api.AuditTarget;
import io.github.jho951.platform.governance.identity.IdentityAuditEvent;
import io.github.jho951.platform.governance.identity.IdentityAuditReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultIdentityAuditRecorderTest {
    @Test
    void recordsStandardIdentityEventWithContextAndRedaction() {
        CapturingAuditLogger auditLogger = new CapturingAuditLogger();
        DefaultIdentityAuditRecorder recorder = new DefaultIdentityAuditRecorder(
                auditLogger,
                new IdentityAuditEventValidator(),
                true,
                true,
                AuditFailurePolicy.FAIL_CLOSED,
                List.of(),
                List.of(),
                List.of(event -> new AuditContext("trace-1", "request-1", "203.0.113.10", "JUnit"))
        );

        recorder.loginFailed(IdentityAuditEvent.loginFailed()
                .actor(AuditActor.anonymous("login@example.com"))
                .loginId("login@example.com")
                .reason(IdentityAuditReason.INVALID_CREDENTIAL)
                .attribute("password", "raw-password")
                .attribute("accessToken", "raw-token")
                .build());

        AuditEvent event = auditLogger.events.get(0);
        assertEquals("IDENTITY_LOGIN_FAILED", event.getAction());
        assertEquals(com.auditlog.api.AuditResult.FAILURE, event.getResult());
        assertEquals("INVALID_CREDENTIAL", event.getReason());
        assertEquals("trace-1", event.getTraceId());
        assertEquals("request-1", event.getRequestId());
        assertEquals("203.0.113.10", event.getClientIp());
        assertEquals("[REDACTED]", event.getDetails().get("password"));
        assertEquals("[REDACTED]", event.getDetails().get("accessToken"));
    }

    @Test
    void loginFailureRequiresClientIpAfterContextResolution() {
        DefaultIdentityAuditRecorder recorder = new DefaultIdentityAuditRecorder(event -> { });

        IdentityAuditValidationException exception = assertThrows(IdentityAuditValidationException.class, () ->
                recorder.loginFailed(IdentityAuditEvent.loginFailed()
                        .actor(AuditActor.anonymous("login@example.com"))
                        .loginId("login@example.com")
                        .reason(IdentityAuditReason.INVALID_CREDENTIAL)
                        .build()));

        assertEquals(List.of("clientIp is required for login failure"), exception.errors());
    }

    @Test
    void signingKeyRotationRequiresOldAndNewKeyIds() {
        DefaultIdentityAuditRecorder recorder = new DefaultIdentityAuditRecorder(event -> { });

        IdentityAuditValidationException exception = assertThrows(IdentityAuditValidationException.class, () ->
                recorder.signingKeyRotated(IdentityAuditEvent.signingKeyRotated()
                        .actor(AuditActor.service("auth-service"))
                        .target(AuditTarget.signingKey("key-2"))
                        .reason(IdentityAuditReason.KEY_ROTATION)
                        .build()));

        assertEquals(List.of(
                "oldKeyId is required for signing key rotation",
                "newKeyId is required for signing key rotation"
        ), exception.errors());
    }

    private static final class CapturingAuditLogger implements AuditLogger {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void log(AuditEvent event) {
            events.add(event);
        }
    }
}
