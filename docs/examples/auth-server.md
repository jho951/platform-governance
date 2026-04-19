# Auth Server Example

auth-server는 identity audit 중심 서비스로 시작하되, 내부 caller 정책이나 admin IP 정책처럼 거부 흐름이 필요한 지점에 governance plugin을 붙인다.

## 설정

```yaml
platform:
  governance:
    service-role-preset: identity-service
    audit:
      service-name: auth-server
      environment: prod
      failure-policy: FAIL_CLOSED
      identity:
        enabled: true
        validation-enabled: true
        fail-on-validation-error: true
    policy-config:
      values:
        auth.internal-caller.allowed: "authz-server,batch-server"
        auth.admin.allowed-cidrs: "10.0.0.0/8"
    engine:
      strict: true
      failure-policy: FAIL_CLOSED
    violation:
      action: DENY
      handler-failure-fatal: true
```

`identity-service` preset은 audit validation을 강하게 유지하면서, identity audit 중심 서비스가 모든 정책 값을 초기에 갖지 않아도 시작할 수 있게 운영 기본값을 보강한다. 명시한 설정은 preset이 덮어쓰지 않는다.

## AuditSink 등록

```java
@Bean
AuditSink auditSink(ExternalAuditClient client) {
    return event -> client.send(event);
}
```

운영 profile에서는 `AuditSink`, `AuditContextResolver`, `audit.service-name`, `audit.environment`가 기본 요구사항이다.

## 정책 plugin

```java
@Bean
GovernancePolicyPlugin internalCallerPolicy(PolicyConfigSource policyConfigSource) {
    return new GovernancePolicyPlugin() {
        @Override
        public String name() {
            return "auth-internal-caller";
        }

        @Override
        public boolean supports(GovernanceRequest request, GovernanceContext context) {
            return "internal-call".equals(request.action());
        }

        @Override
        public GovernanceVerdict evaluate(GovernanceRequest request, GovernanceContext context) {
            String allowed = policyConfigSource.resolve("auth.internal-caller.allowed").orElse("");
            if (allowed.contains(request.subject())) {
                return GovernanceVerdict.allow(name(), "caller allowed");
            }
            return GovernanceVerdict.deny(
                    name(),
                    "caller not allowed",
                    new PolicyEvidence("auth.internal-caller.allowed", null, "DENY", "caller-allow-list", "INTERNAL_CALLER_DENIED")
            );
        }
    };
}
```

서비스별 판단은 `GovernancePolicyPlugin` 또는 `GovernanceDecisionEngine`으로 교체한다. `GovernancePolicyService`는 platform wrapper가 audit과 violation handling을 유지하는 바깥 골격이다.

## DENY flow

```java
GovernanceVerdict verdict = governancePolicyService.evaluate(
        new GovernanceRequest("unknown-service", "/internal/token", "internal-call", Map.of(), Instant.now()),
        new GovernanceContext("auth-server", "prod", Map.of())
);

if (!verdict.allowed()) {
    throw new AccessDeniedException(verdict.reason());
}
```

기본 wrapper는 `request.subject`, `request.resource`, `request.action`, `context.actor`, `context.environment`, `audit.service-name`, `audit.environment`, `policy.id`, `policy.evidence.*`, `governance.decision`을 audit에 남긴다. 정책 source 전체 snapshot은 남기지 않는다.

## Policy change flow

```java
policyChangeRecorder.record(new PolicyChangeEvent(
        "operator-1",
        "auth.internal-caller.allowed",
        "authz-server",
        "authz-server,batch-server",
        "allow batch token rotation",
        Map.of("ticket", "SEC-1234"),
        Instant.now()
));
```

`password`, `secret`, `token`, `cookie` 계열 key와 민감한 policy key의 previous/new value는 redaction된다.

## Prod fail-fast 해소

remote policy source가 snapshot을 제공하지 않지만 key resolve는 정상이라면 capability를 분리한다.

```java
final class RemotePolicyConfigSource implements PolicyConfigSource {
    @Override
    public Optional<String> resolve(String key) {
        return remoteClient.get(key);
    }

    @Override
    public Map<String, String> snapshot() {
        return Map.of();
    }

    @Override
    public boolean supportsSnapshot() {
        return false;
    }

    @Override
    public PolicyConfigOperationalStatus operationalStatus() {
        if (remoteClient.isHealthy()) {
            return PolicyConfigOperationalStatus.operational("remote policy source healthy");
        }
        return PolicyConfigOperationalStatus.unavailable("remote policy source health check failed");
    }
}
```

정책을 코드 plugin만으로 판단하고 config source가 필요 없는 서비스는 `platform.governance.operational.require-policy-config-in-enforcing-mode=false`를 명시한다.
