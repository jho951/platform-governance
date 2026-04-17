package io.github.jho951.platform.governance.api;

public record AuditCorrelation(
        String traceId,
        String spanId,
        String requestId,
        String correlationId,
        String clientIp,
        String userAgent,
        String serviceName,
        String environment,
        String host,
        String route,
        String httpMethod
) {
    public static final AuditCorrelation EMPTY = builder().build();

    public AuditCorrelation {
        traceId = normalize(traceId);
        spanId = normalize(spanId);
        requestId = normalize(requestId);
        correlationId = normalize(correlationId);
        clientIp = normalize(clientIp);
        userAgent = normalize(userAgent);
        serviceName = normalize(serviceName);
        environment = normalize(environment);
        host = normalize(host);
        route = normalize(route);
        httpMethod = normalize(httpMethod);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder()
                .traceId(traceId)
                .spanId(spanId)
                .requestId(requestId)
                .correlationId(correlationId)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .serviceName(serviceName)
                .environment(environment)
                .host(host)
                .route(route)
                .httpMethod(httpMethod);
    }

    public AuditCorrelation mergeMissing(AuditCorrelation fallback) {
        if (fallback == null) {
            return this;
        }
        return builder()
                .traceId(firstPresent(traceId, fallback.traceId))
                .spanId(firstPresent(spanId, fallback.spanId))
                .requestId(firstPresent(requestId, fallback.requestId))
                .correlationId(firstPresent(correlationId, fallback.correlationId))
                .clientIp(firstPresent(clientIp, fallback.clientIp))
                .userAgent(firstPresent(userAgent, fallback.userAgent))
                .serviceName(firstPresent(serviceName, fallback.serviceName))
                .environment(firstPresent(environment, fallback.environment))
                .host(firstPresent(host, fallback.host))
                .route(firstPresent(route, fallback.route))
                .httpMethod(firstPresent(httpMethod, fallback.httpMethod))
                .build();
    }

    private static String firstPresent(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class Builder {
        private String traceId;
        private String spanId;
        private String requestId;
        private String correlationId;
        private String clientIp;
        private String userAgent;
        private String serviceName;
        private String environment;
        private String host;
        private String route;
        private String httpMethod;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder route(String route) {
            this.route = route;
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        public AuditCorrelation build() {
            return new AuditCorrelation(
                    traceId,
                    spanId,
                    requestId,
                    correlationId,
                    clientIp,
                    userAgent,
                    serviceName,
                    environment,
                    host,
                    route,
                    httpMethod
            );
        }
    }
}
