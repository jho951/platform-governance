package io.github.jho951.platform.governance.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AuditTarget(
        Type type,
        String id,
        Map<String, String> attributes
) {
    public AuditTarget {
        type = type == null ? Type.UNKNOWN : type;
        id = normalize(id);
        attributes = attributes == null ? Collections.emptyMap() : Map.copyOf(attributes);
    }

    public static AuditTarget authAccount(String userId) {
        return new AuditTarget(Type.AUTH_ACCOUNT, userId, Map.of());
    }

    public static AuditTarget session(String sessionId) {
        return new AuditTarget(Type.SESSION, sessionId, Map.of());
    }

    public static AuditTarget token(String tokenId) {
        return new AuditTarget(Type.TOKEN, tokenId, Map.of());
    }

    public static AuditTarget refreshTokenFingerprint(String fingerprint) {
        return new AuditTarget(Type.TOKEN, fingerprint, Map.of("token.kind", "refresh", "token.identifier", "fingerprint"));
    }

    public static AuditTarget signingKey(String keyId) {
        return new AuditTarget(Type.SIGNING_KEY, keyId, Map.of());
    }

    public static AuditTarget unknown() {
        return new AuditTarget(Type.UNKNOWN, null, Map.of());
    }

    public AuditTarget withAttribute(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return this;
        }
        Map<String, String> updated = new LinkedHashMap<>(attributes);
        updated.put(key.trim(), value);
        return new AuditTarget(type, id, updated);
    }

    public boolean hasId() {
        return id != null && !id.isBlank();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public enum Type {
        AUTH_ACCOUNT,
        SESSION,
        TOKEN,
        SIGNING_KEY,
        POLICY,
        SERVICE,
        UNKNOWN
    }
}
