package io.github.jho951.platform.governance.api;

public record PolicyEvidence(
        String policyId,
        String policyVersion,
        String decision,
        String ruleId,
        String violationCode
) {
    public PolicyEvidence {
        policyId = normalize(policyId);
        policyVersion = normalize(policyVersion);
        decision = normalize(decision);
        ruleId = normalize(ruleId);
        violationCode = normalize(violationCode);
    }

    public static PolicyEvidence empty() {
        return new PolicyEvidence(null, null, null, null, null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
