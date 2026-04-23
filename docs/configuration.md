# Configuration

`platform-governance`는 `platform.governance.*` prefix로 설정한다.
3계층 서비스는 이 prefix 아래에서 자기 서비스에 필요한 정책 값만 선언하고, 2계층의 기본 실행 골격을 그대로 사용한다.

## 기본 속성

- `enabled`
- `service-role-preset`
- `audit.enabled`
- `audit.service-name`
- `audit.environment`
- `audit.failure-policy`
- `audit.identity.enabled`
- `audit.identity.validation-enabled`
- `audit.identity.fail-on-validation-error`
- `policy-config.values.*`
- `feature-flags.store`
- `feature-flags.file-path`
- `feature-flags.cache-ttl-millis`
- `engine.strict`
- `engine.failure-policy`
- `violation.action`
- `violation.handler-failure-fatal`
- `operational.fail-fast-enabled`
- `operational.production-profiles`
- `operational.allow-audit-disabled-in-production`
- `operational.allow-non-strict-engine-in-production`
- `operational.allow-permissive-violation-action-in-production`
- `operational.require-audit-sink-in-production`
- `operational.require-audit-context-resolver-in-production`
- `operational.require-audit-service-identity-in-production`
- `operational.require-identity-audit-validation-in-production`
- `operational.require-policy-config-in-enforcing-mode`
- `operational.require-fatal-handler-failures-in-production`
- `operational.allow-ignore-audit-failure-policy-in-production`

## 서비스 역할 preset

`service-role-preset`은 3계층 서비스의 주 governance 성격을 선언한다.

| Preset | 용도 |
| --- | --- |
| `GENERAL` | 직접 모든 설정을 고르는 기본값 |
| `IDENTITY_SERVICE` | `auth-service` 같은 identity audit 중심 서비스 |
| `POLICY_DECISION_SERVICE` | `authz-service` 같은 정책 결정 서비스 |
| `RESOURCE_SERVICE` | `user-service`, `editor-service` 같은 resource API 서비스 |
| `OBSERVABILITY_SERVICE` | `monitoring-service` 같은 감사/관측 중심 서비스 |

Preset은 명시적으로 설정한 값은 존중하고, 설정하지 않은 운영 기본값만 보강한다.

예:

```yaml
platform:
  governance:
    service-role-preset: policy-decision-service
    audit:
      service-name: authz-service
      environment: prod
```

권장 매핑:

| 3계층 서비스 | Preset |
| --- | --- |
| `auth-service` | `identity-service` |
| `authz-service` | `policy-decision-service` |
| `user-service` | `resource-service` |
| `editor-service` | `resource-service` |
| `monitoring-service` | `observability-service` |

Preset별 운영 기본값:

| Preset | 운영 기본값 방향 |
| --- | --- |
| `IDENTITY_SERVICE` | identity audit validation과 fail-closed audit을 유지하고, audit 중심 서비스가 config source 없이도 시작 가능하게 완화 |
| `POLICY_DECISION_SERVICE` | strict engine, `DENY` violation, fatal handler failure를 기본으로 강제 |
| `RESOURCE_SERVICE` | resource API의 정책 평가를 enforcing 방향으로 유지 |
| `OBSERVABILITY_SERVICE` | audit/alert 중심 운영을 허용하되 service identity와 sink 요구사항은 유지 |
| `GENERAL` | preset 보강 없이 명시 설정을 그대로 사용 |

## 기본 동작

- audit는 기본 활성화
- audit logger는 `audit-log-core`의 기본 구현을 사용하고, 등록된 `AuditSink`가 있으면 composite delivery를 사용한다.
- 등록된 `AuditSink`가 없으면 `platform-governance-adapter-auditlog`의 `LoggingAuditSink` fallback으로 governance audit을 애플리케이션 로그에 남긴다.
- identity audit는 `IdentityAuditRecorder` 공개 API를 제공하고 내부에서 audit library의 `AuditLogger`로 매핑한다.
- `MdcAuditContextResolver`가 MDC의 `traceId`, `requestId`, `clientIp`, `userAgent`를 기본 correlation 값으로 연결한다.
- policy config는 `policy-config`의 `PolicyResolver` 기반 기본 소스를 제공한다.
- `feature-flags` 설정은 `FeatureFlagClient` 기본 bean을 제공하기 위한 config 호환 기준으로 사용한다.
- 기존 `plugin-policy-engine.*` prefix는 2.0.1 deprecated alias이며 3.1.0에서 제거한다.
- `feature-flags.*`와 `plugin-policy-engine.*`를 동시에 사용하면 profile과 무관하게 시작에 실패한다.
- governance decision engine은 기본적으로 등록된 `GovernancePolicyPlugin`을 bean 순서대로 순회한다.
- 정말 전사 공통인 불변 규칙만 기본 강제로 둔다.
- 서비스별 차이가 필요한 값은 `platform.governance.*` 설정 또는 서비스가 등록한 bean으로 override한다.
- policy change recorder는 기본적으로 audit에 정책 변경 이벤트를 기록한다.
- violation handler는 기본적으로 audit에 위반 대응 결과를 기록한다.
- violation handler 실패는 기본적으로 감사에 남기고 원래 verdict를 유지한다.
- 외부 `AuditLogRecorder` bean은 기본적으로 무시한다. 서비스 확장은 `AuditSink`를 사용한다.
- `GovernancePolicyService` bean 직접 등록은 지원하지 않으며 시작 시 실패한다.
- `prod` 또는 `production` profile에서는 운영 위험 설정을 기본 fail-fast로 막는다.

## 운영 fail-fast

다음 설정은 profile과 무관하게 항상 검증한다.

- `feature-flags.store=FILE`이면 `feature-flags.file-path`가 필요하다.
- `feature-flags.*`와 legacy `plugin-policy-engine.*`를 동시에 설정하면 실패한다.

`operational.fail-fast-enabled=true`이면 다음 운영 위험 설정을 시작 시점에 검증한다.

- 운영 profile에서 `audit.enabled=false`는 기본적으로 실패한다.
- 운영 profile에서 `engine.strict=false`는 기본적으로 실패한다.
- 운영 profile에서 위반 대응 강도가 약한 `violation.action=AUDIT_ONLY` 또는 `ALERT`는 기본적으로 실패한다.
- 운영 profile에서 명시적 `AuditSink` bean이 하나도 없으면 기본적으로 실패한다.
- 운영 profile에서 `AuditContextResolver` bean이 하나도 없으면 기본적으로 실패한다.
- 운영 profile에서 `audit.service-name` 또는 `audit.environment`가 없으면 기본적으로 실패한다.
- active profile과 `audit.environment`의 운영/비운영 의미가 충돌하면 기본적으로 실패한다.
- 운영 profile에서 `audit.identity.validation-enabled=false`는 기본적으로 실패한다.
- 운영 profile에서 `audit.failure-policy=IGNORE`는 기본적으로 실패한다.
- 운영 profile에서 거부 대응 action(`DENY`, `ESCALATE`)을 쓰면 policy config source의 `operationalStatus()`가 `OPERATIONAL`이 아닐 때 기본적으로 실패한다.
- policy config source가 snapshot을 지원하는 경우 snapshot이 비어 있어도 기본적으로 실패한다. snapshot을 지원하지 않는 remote/lazy source는 `supportsSnapshot=false`, `operationalStatus=OPERATIONAL`로 capability를 분리한다.
- 운영 profile에서 `violation.handler-failure-fatal=false`는 기본적으로 실패한다.

운영 감사 출력 대상의 공식 production SPI는 `AuditSink` bean이다.
`platform-governance-adapter-auditlog`는 ready-made `LoggingAuditSink`를 제공하며, explicit sink가 없을 때 dev/test 기본 fallback으로 사용한다.
`AuditLogRecorder`는 governance event를 audit pipeline으로 넘기는 내부 adapter다. 서비스 확장점은 `AuditSink`이며, 외부 `AuditLogRecorder` 구현은 mainline starter에서 무시한다.

서비스가 의도적으로 다른 운영 정책을 선택해야 하면 `operational.allow-*` 또는 `operational.require-*` 속성으로 명시적으로 완화한다.
`operational.fail-fast-enabled=false`는 운영 위험 설정 검증의 마지막 탈출구로만 사용하며, 항상 검증 항목은 우회하지 않는다.

## Violation action

`GovernanceVerdict`가 최종 `ALLOW`/`DENY` 평가 결과를 결정한다.
`ViolationAction`은 `DENY` verdict가 나온 뒤 실행할 위반 대응 방식이며, verdict를 `ALLOW`로 바꾸지 않는다.

- `AUDIT_ONLY`: 감사 중심의 약한 위반 대응
- `ALERT`: 알림/감사 중심의 약한 위반 대응
- `DENY`: 거부 verdict에 맞춘 기본 위반 대응
- `ESCALATE`: 거부 verdict와 함께 상위 대응을 실행하는 강한 위반 대응

## Engine failure policy

`engine.failure-policy`는 plugin 실행 중 예외가 발생했을 때의 verdict 의미를 정한다.

- `FAIL_CLOSED`: plugin exception을 `DENY`로 반환한다.
- `FAIL_OPEN`: plugin exception을 `ALLOW`로 반환하되 reason에 실패 정보를 남긴다.
- `AUDIT_AND_DENY`: 2.0.1 deprecated alias다. `FAIL_CLOSED`와 동일하게 동작하므로 새 설정은 `FAIL_CLOSED`를 사용한다. 3.1.0에서 제거한다.

## 설정 예시

```yaml
platform:
  governance:
    audit:
      enabled: true
      service-name: auth-service
      environment: prod
      failure-policy: FAIL_CLOSED
      identity:
        enabled: true
        validation-enabled: true
        fail-on-validation-error: true
    policy-config:
      values:
        feature.review.required: "true"
    feature-flags:
      store: memory
      cache-ttl-millis: 3000
    engine:
      strict: false
      failure-policy: FAIL_CLOSED
    violation:
      action: DENY
      handler-failure-fatal: false
    operational:
      fail-fast-enabled: true
      production-profiles:
        - prod
        - production
      allow-audit-disabled-in-production: false
      allow-non-strict-engine-in-production: false
      allow-permissive-violation-action-in-production: false
      require-audit-sink-in-production: true
      require-audit-context-resolver-in-production: true
      require-audit-service-identity-in-production: true
      require-identity-audit-validation-in-production: true
      require-policy-config-in-enforcing-mode: true
      require-fatal-handler-failures-in-production: true
      allow-ignore-audit-failure-policy-in-production: false
```
