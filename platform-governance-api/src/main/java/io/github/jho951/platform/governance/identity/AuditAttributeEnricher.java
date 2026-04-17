package io.github.jho951.platform.governance.identity;

import java.util.Map;

public interface AuditAttributeEnricher {
    Map<String, String> enrich(IdentityAuditEvent event);
}
