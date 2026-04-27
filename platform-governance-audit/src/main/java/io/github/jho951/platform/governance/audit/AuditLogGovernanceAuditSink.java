package io.github.jho951.platform.governance.audit;

import com.auditlog.api.AuditActorType;
import com.auditlog.api.AuditEvent;
import com.auditlog.api.AuditEventType;
import com.auditlog.api.AuditResult;
import com.auditlog.api.AuditSink;
import com.auditlog.core.AsyncAuditSink;
import com.auditlog.core.CompositeAuditSink;
import com.auditlog.core.FileAuditSink;
import com.auditlog.core.HttpAuditSink;
import io.github.jho951.platform.governance.api.AuditEntry;
import io.github.jho951.platform.governance.api.GovernanceAuditSink;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Bridges platform governance audit entries to the durable audit-log sinks.
 */
public final class AuditLogGovernanceAuditSink implements GovernanceAuditSink, AutoCloseable {
    private final AuditSink delegate;

    public AuditLogGovernanceAuditSink(AuditSink delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static AuditLogGovernanceAuditSink durable(
            Path filePath,
            String serviceName,
            String environment,
            boolean asyncEnabled,
            int asyncThreadCount,
            int asyncQueueCapacity,
            URI httpEndpoint,
            String apiKey
    ) {
        Objects.requireNonNull(filePath, "filePath");
        List<AuditSink> sinks = new ArrayList<>();
        sinks.add(new FileAuditSink(filePath, serviceName, environment));
        if (httpEndpoint != null) {
            sinks.add(new HttpAuditSink(httpEndpoint, serviceName, environment, apiKey));
        }

        AuditSink sink = sinks.size() == 1 ? sinks.get(0) : new CompositeAuditSink(sinks);
        if (asyncEnabled) {
            sink = new AsyncAuditSink(
                    sink,
                    auditLogExecutor(asyncThreadCount, asyncQueueCapacity, serviceName)
            );
        }
        return new AuditLogGovernanceAuditSink(sink);
    }

    @Override
    public void write(AuditEntry entry) {
        delegate.write(toAuditEvent(entry));
    }

    @Override
    public void close() {
        delegate.close();
    }

    static AuditEvent toAuditEvent(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry");
        Map<String, String> attributes = entry.attributes() == null ? Map.of() : entry.attributes();
        AuditEvent.Builder builder = AuditEvent.builder(
                resolveEventType(attributes.get("eventType")),
                safe(entry.message())
        );

        if (entry.occurredAt() != null) {
            builder.occurredAt(entry.occurredAt());
        }

        putIfPresent(builder::actorId, attributes.get("actorId"));
        putIfPresent(builder::actorName, attributes.get("actorName"));
        AuditActorType actorType = resolveActorType(attributes.get("actorType"));
        if (actorType != AuditActorType.UNKNOWN) {
            builder.actorType(actorType);
        }

        String resourceType = attributes.get("resourceType");
        String resourceId = attributes.get("resourceId");
        if (hasText(resourceType) || hasText(resourceId)) {
            builder.resource(resourceType, resourceId);
        }

        AuditResult result = resolveResult(attributes.get("result"));
        if (result != null) {
            builder.result(result);
        }

        putIfPresent(builder::reason, attributes.get("reason"));
        putIfPresent(builder::traceId, firstNonBlank(attributes, "traceId", "audit.trace-id"));
        putIfPresent(builder::requestId, firstNonBlank(attributes, "requestId", "audit.request-id"));
        putIfPresent(builder::clientIp, firstNonBlank(attributes, "clientIp", "audit.client-ip"));
        putIfPresent(builder::userAgent, firstNonBlank(attributes, "userAgent", "audit.user-agent"));

        auditDetails(entry.category(), attributes).forEach(builder::detail);
        return builder.build();
    }

    private static Map<String, Object> auditDetails(String category, Map<String, String> attributes) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        if (hasText(category)) {
            details.put("category", category);
        }
        attributes.forEach((key, value) -> {
            if (hasText(key) && value != null) {
                details.put(key, value);
            }
        });
        return details;
    }

    private static ExecutorService auditLogExecutor(int asyncThreadCount, int asyncQueueCapacity, String serviceName) {
        int threadCount = Math.max(1, asyncThreadCount);
        int queueCapacity = Math.max(1, asyncQueueCapacity);
        String threadNamePrefix = (serviceName == null || serviceName.isBlank() ? "governance" : serviceName)
                + "-governance-audit-";
        return new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName(threadNamePrefix + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static AuditEventType resolveEventType(String rawEventType) {
        if (!hasText(rawEventType)) {
            return AuditEventType.CUSTOM;
        }
        try {
            return AuditEventType.valueOf(rawEventType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AuditEventType.CUSTOM;
        }
    }

    private static AuditActorType resolveActorType(String rawActorType) {
        if (!hasText(rawActorType)) {
            return AuditActorType.UNKNOWN;
        }
        try {
            return AuditActorType.valueOf(rawActorType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AuditActorType.UNKNOWN;
        }
    }

    private static AuditResult resolveResult(String rawResult) {
        if (!hasText(rawResult)) {
            return null;
        }
        try {
            return AuditResult.valueOf(rawResult.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(Map<String, String> attributes, String... keys) {
        for (String key : keys) {
            String value = attributes.get(key);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static void putIfPresent(java.util.function.Consumer<String> consumer, String value) {
        if (hasText(value)) {
            consumer.accept(value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
