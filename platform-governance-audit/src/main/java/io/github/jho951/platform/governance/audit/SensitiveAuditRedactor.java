package io.github.jho951.platform.governance.audit;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SensitiveAuditRedactor {
    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwordhash",
            "accesstoken",
            "refreshtoken",
            "authorizationcode",
            "clientsecret",
            "jwtsecret",
            "cookie",
            "set-cookie"
    );

    private SensitiveAuditRedactor() {
    }

    public static Map<String, Object> redact(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> redacted = new LinkedHashMap<>();
        attributes.forEach((key, value) -> {
            if (isSensitive(key)) {
                redacted.put(key, REDACTED);
            } else {
                redacted.put(key, value);
            }
        });
        return redacted;
    }

    public static Map<String, String> redactStrings(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, String> redacted = new LinkedHashMap<>();
        attributes.forEach((key, value) -> redacted.put(key, redactValue(key, value)));
        return redacted;
    }

    public static String redactValue(String key, String value) {
        return isSensitive(key) ? REDACTED : value;
    }

    public static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.endsWith("password")
                || normalized.endsWith("secret")
                || normalized.endsWith("token")
                || normalized.endsWith("cookie");
    }
}
