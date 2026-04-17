package io.github.jho951.platform.governance.api;

public record AuditActor(
        Type type,
        String id,
        String displayName
) {
    public AuditActor {
        type = type == null ? Type.UNKNOWN : type;
        id = normalize(id);
        displayName = normalize(displayName);
    }

    public static AuditActor service(String serviceName) {
        return new AuditActor(Type.SERVICE, serviceName, serviceName);
    }

    public static AuditActor user(String userId) {
        return new AuditActor(Type.USER, userId, null);
    }

    public static AuditActor admin(String adminId) {
        return new AuditActor(Type.ADMIN, adminId, null);
    }

    public static AuditActor anonymous(String loginId) {
        return new AuditActor(Type.ANONYMOUS, loginId, null);
    }

    public static AuditActor unknown() {
        return new AuditActor(Type.UNKNOWN, null, null);
    }

    public boolean hasId() {
        return id != null && !id.isBlank();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public enum Type {
        USER,
        ADMIN,
        SYSTEM,
        SERVICE,
        ANONYMOUS,
        UNKNOWN
    }
}
