package io.github.jho951.platform.governance.spring;

import com.auditlog.api.AuditContext;
import com.auditlog.api.AuditEvent;
import com.auditlog.spi.AuditContextResolver;

import java.lang.reflect.Method;

final class MdcAuditContextResolver implements AuditContextResolver {
    @Override
    public AuditContext resolve(AuditEvent event) {
        return new AuditContext(
                firstMdcValue("traceId", "trace_id", "trace.id"),
                firstMdcValue("requestId", "request_id", "request.id", "x-request-id"),
                firstMdcValue("clientIp", "client_ip", "client.ip", "remoteAddr"),
                firstMdcValue("userAgent", "user_agent", "user.agent")
        );
    }

    private static String firstMdcValue(String... keys) {
        for (String key : keys) {
            String value = mdcValue(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String mdcValue(String key) {
        try {
            Class<?> mdcClass = Class.forName("org.slf4j.MDC");
            Method get = mdcClass.getMethod("get", String.class);
            Object value = get.invoke(null, key);
            return value instanceof String text ? text : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
