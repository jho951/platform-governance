package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditContext;
import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditLogger;
import com.auditlog.spi.AuditContextResolver;
import io.github.jho951.platform.governance.api.AuditCorrelation;
import io.github.jho951.platform.governance.api.AuditActor;
import io.github.jho951.platform.governance.api.AuditFailurePolicy;
import io.github.jho951.platform.governance.api.AuditResult;
import io.github.jho951.platform.governance.api.AuditTarget;
import io.github.jho951.platform.governance.api.PolicyEvidence;
import io.github.jho951.platform.governance.identity.AuditAttributeEnricher;
import io.github.jho951.platform.governance.identity.IdentityAuditAction;
import io.github.jho951.platform.governance.identity.IdentityAuditCustomizer;
import io.github.jho951.platform.governance.identity.IdentityAuditEvent;
import io.github.jho951.platform.governance.identity.IdentityAuditRecorder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DefaultIdentityAuditRecorder implements IdentityAuditRecorder {
    private static final Logger LOGGER = Logger.getLogger(DefaultIdentityAuditRecorder.class.getName());

    private final AuditLogger auditLogger;
    private final IdentityAuditEventValidator validator;
    private final boolean validationEnabled;
    private final boolean failOnValidationError;
    private final AuditFailurePolicy failurePolicy;
    private final List<IdentityAuditCustomizer> customizers;
    private final List<AuditAttributeEnricher> enrichers;
    private final List<AuditContextResolver> contextResolvers;

    public DefaultIdentityAuditRecorder(AuditLogger auditLogger) {
        this(auditLogger, new IdentityAuditEventValidator(), true, true, AuditFailurePolicy.FAIL_CLOSED, List.of(), List.of());
    }

    public DefaultIdentityAuditRecorder(
            AuditLogger auditLogger,
            IdentityAuditEventValidator validator,
            boolean validationEnabled,
            boolean failOnValidationError,
            AuditFailurePolicy failurePolicy,
            List<IdentityAuditCustomizer> customizers,
            List<AuditAttributeEnricher> enrichers
    ) {
        this(auditLogger, validator, validationEnabled, failOnValidationError, failurePolicy, customizers, enrichers, List.of());
    }

    public DefaultIdentityAuditRecorder(
            AuditLogger auditLogger,
            IdentityAuditEventValidator validator,
            boolean validationEnabled,
            boolean failOnValidationError,
            AuditFailurePolicy failurePolicy,
            List<IdentityAuditCustomizer> customizers,
            List<AuditAttributeEnricher> enrichers,
            List<AuditContextResolver> contextResolvers
    ) {
        this.auditLogger = Objects.requireNonNull(auditLogger, "auditLogger");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.validationEnabled = validationEnabled;
        this.failOnValidationError = failOnValidationError;
        this.failurePolicy = failurePolicy == null ? AuditFailurePolicy.FAIL_CLOSED : failurePolicy;
        this.customizers = customizers == null ? List.of() : List.copyOf(customizers);
        this.enrichers = enrichers == null ? List.of() : List.copyOf(enrichers);
        this.contextResolvers = contextResolvers == null ? List.of() : List.copyOf(contextResolvers);
    }

    @Override
    public void record(IdentityAuditEvent event) {
        IdentityAuditEvent customized = enrichContext(applyCustomizers(Objects.requireNonNull(event, "event")));
        validate(customized);
        AuditEvent auditEvent = toAuditEvent(customized);
        write(auditEvent);
    }

    private IdentityAuditEvent applyCustomizers(IdentityAuditEvent event) {
        IdentityAuditEvent current = event;
        for (IdentityAuditCustomizer customizer : customizers) {
            IdentityAuditEvent customized = customizer.customize(current);
            if (customized != null) {
                current = customized;
            }
        }
        return current;
    }

    private IdentityAuditEvent enrichContext(IdentityAuditEvent event) {
        if (contextResolvers.isEmpty()) {
            return event;
        }
        AuditEvent probe = toAuditEvent(event);
        AuditCorrelation context = AuditCorrelation.EMPTY;
        for (AuditContextResolver resolver : contextResolvers) {
            AuditContext resolved = resolver.resolve(probe);
            if (resolved == null) {
                continue;
            }
            context = context.mergeMissing(AuditCorrelation.builder()
                    .traceId(resolved.traceId())
                    .requestId(resolved.requestId())
                    .clientIp(resolved.clientIp())
                    .userAgent(resolved.userAgent())
                    .build());
        }
        return event.toBuilder()
                .correlation(event.correlation().mergeMissing(context))
                .build();
    }

    private void validate(IdentityAuditEvent event) {
        if (!validationEnabled) {
            return;
        }
        List<String> errors = validator.validate(event);
        if (errors.isEmpty()) {
            return;
        }
        IdentityAuditValidationException exception = new IdentityAuditValidationException(event.action(), errors);
        if (failOnValidationError) {
            throw exception;
        }
        LOGGER.log(Level.WARNING, exception.getMessage());
    }

    private void write(AuditEvent auditEvent) {
        try {
            auditLogger.log(auditEvent);
        } catch (RuntimeException exception) {
            handleFailure(auditEvent, exception);
        }
    }

    private void handleFailure(AuditEvent auditEvent, RuntimeException exception) {
        if (failurePolicy == AuditFailurePolicy.IGNORE) {
            return;
        }
        if (failurePolicy == AuditFailurePolicy.FAIL_OPEN_WITH_ALERT) {
            LOGGER.log(Level.SEVERE, "identity audit recording failed: " + auditEvent.getAction(), exception);
            return;
        }
        if (failurePolicy == AuditFailurePolicy.RETRY_THEN_FAIL) {
            auditLogger.log(auditEvent);
            return;
        }
        throw exception;
    }

    private AuditEvent toAuditEvent(IdentityAuditEvent event) {
        Map<String, Object> details = details(event);
        AuditEvent.Builder builder = AuditEvent.builder(toAuditEventType(event.action()), event.action().code())
                .actor(event.actor().id(), toAuditActorType(event.actor()), event.actor().displayName())
                .resource(toResourceType(event.target()), event.target().id())
                .result(toAuditResult(event.result()))
                .details(SensitiveAuditRedactor.redact(details))
                .occurredAt(event.occurredAt());

        if (event.reason() != null) {
            builder.reason(event.reason().code());
        }
        if (event.correlation().traceId() != null) {
            builder.traceId(event.correlation().traceId());
        }
        if (event.correlation().requestId() != null) {
            builder.requestId(event.correlation().requestId());
        }
        if (event.correlation().clientIp() != null) {
            builder.clientIp(event.correlation().clientIp());
        }
        if (event.correlation().userAgent() != null) {
            builder.userAgent(event.correlation().userAgent());
        }
        return builder.build();
    }

    private Map<String, Object> details(IdentityAuditEvent event) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("category", event.action().category().name());
        details.put("severity", event.severity().name());
        details.put("action", event.action().code());
        details.put("result", event.result().name());
        if (event.reason() != null) {
            details.put("reason", event.reason().code());
        }
        putActor(details, event.actor());
        putTarget(details, event.target());
        putIfPresent(details, "channel", event.channel());
        putIfPresent(details, "provider", event.provider());
        putIfPresent(details, "loginId", event.loginId());
        putCorrelation(details, event);
        putPolicyEvidence(details, event.policyEvidence());
        details.putAll(event.attributes());
        for (AuditAttributeEnricher enricher : enrichers) {
            Map<String, String> enriched = enricher.enrich(event);
            if (enriched != null) {
                enriched.forEach((key, value) -> putIfPresent(details, key, value));
            }
        }
        return details;
    }

    private static void putActor(Map<String, Object> details, AuditActor actor) {
        details.put("actor.type", actor.type().name());
        putIfPresent(details, "actor.id", actor.id());
        putIfPresent(details, "actor.displayName", actor.displayName());
    }

    private static void putTarget(Map<String, Object> details, AuditTarget target) {
        details.put("target.type", target.type().name());
        putIfPresent(details, "target.id", target.id());
        target.attributes().forEach((key, value) -> putIfPresent(details, "target." + key, value));
    }

    private static void putCorrelation(Map<String, Object> details, IdentityAuditEvent event) {
        putIfPresent(details, "traceId", event.correlation().traceId());
        putIfPresent(details, "spanId", event.correlation().spanId());
        putIfPresent(details, "requestId", event.correlation().requestId());
        putIfPresent(details, "correlationId", event.correlation().correlationId());
        putIfPresent(details, "clientIp", event.correlation().clientIp());
        putIfPresent(details, "userAgent", event.correlation().userAgent());
        putIfPresent(details, "serviceName", event.correlation().serviceName());
        putIfPresent(details, "environment", event.correlation().environment());
        putIfPresent(details, "host", event.correlation().host());
        putIfPresent(details, "route", event.correlation().route());
        putIfPresent(details, "httpMethod", event.correlation().httpMethod());
    }

    private static void putPolicyEvidence(Map<String, Object> details, PolicyEvidence evidence) {
        putIfPresent(details, "policyId", evidence.policyId());
        putIfPresent(details, "policyVersion", evidence.policyVersion());
        putIfPresent(details, "policyDecision", evidence.decision());
        putIfPresent(details, "policyRuleId", evidence.ruleId());
        putIfPresent(details, "policyViolationCode", evidence.violationCode());
    }

    private static void putIfPresent(Map<String, Object> details, String key, String value) {
        if (key != null && !key.isBlank() && value != null) {
            details.put(key, value);
        }
    }

    private static AuditActorType toAuditActorType(AuditActor actor) {
        return switch (actor.type()) {
            case USER -> AuditActorType.USER;
            case ADMIN -> AuditActorType.ADMIN;
            case SYSTEM -> AuditActorType.SYSTEM;
            case SERVICE -> AuditActorType.SERVICE;
            case ANONYMOUS -> AuditActorType.ANONYMOUS;
            case UNKNOWN -> AuditActorType.UNKNOWN;
        };
    }

    private static com.auditlog.api.AuditResult toAuditResult(AuditResult result) {
        if (result == AuditResult.SUCCESS) {
            return com.auditlog.api.AuditResult.SUCCESS;
        }
        return com.auditlog.api.AuditResult.FAILURE;
    }

    private static AuditEventType toAuditEventType(IdentityAuditAction action) {
        return switch (action) {
            case IDENTITY_LOGIN_SUCCEEDED, IDENTITY_LOGIN_FAILED -> AuditEventType.LOGIN;
            case IDENTITY_LOGOUT -> AuditEventType.LOGOUT;
            case IDENTITY_TOKEN_ISSUED, IDENTITY_SESSION_CREATED -> AuditEventType.CREATE;
            case IDENTITY_TOKEN_REFRESHED, IDENTITY_CREDENTIAL_RESET, IDENTITY_SIGNING_KEY_ROTATED -> AuditEventType.UPDATE;
            case IDENTITY_TOKEN_REVOKED, IDENTITY_SESSION_FORCE_LOGOUT, IDENTITY_ADMIN_SESSION_TERMINATED -> AuditEventType.DELETE;
            case IDENTITY_TOKEN_REUSE_DETECTED, IDENTITY_INTERNAL_CALLER_DENIED, IDENTITY_ADMIN_IP_DENIED -> AuditEventType.SYSTEM;
        };
    }

    private static String toResourceType(AuditTarget target) {
        return "IDENTITY_" + target.type().name();
    }
}
