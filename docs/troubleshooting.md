# Troubleshooting

## 운영 profile에서 시작 실패

`prod` profile에서는 `OperationalGovernancePolicyEnforcer`가 위험한 설정을 fail-fast로 막는다.
에러 메시지의 `platform.governance.*` 항목을 먼저 확인한다.

먼저 확인할 값:

- `platform.governance.service-role-preset`
- `platform.governance.audit.service-name`
- `platform.governance.audit.environment`
- `platform.governance.engine.strict`
- `platform.governance.violation.handler-failure-fatal`

서비스 성격과 preset이 맞지 않으면 기본 운영정책이 과하거나 약하게 적용될 수 있다.

## Audit sink 누락

명시적 `AuditSink` bean이 없으면 starter는 `LoggingAuditSink` fallback으로 governance audit을 애플리케이션 로그에 남긴다.
운영 profile에서 `platform.governance.operational.require-audit-sink-in-production=true`인데 명시적 `AuditSink` bean이 없으면 기본적으로 시작에 실패한다.
운영 감사 출력 대상은 `AuditSink`로 등록한다. 로그 기반 delivery만 필요하면 `platform-governance-adapter-auditlog`의 `LoggingAuditSink`를 bean으로 직접 등록해도 된다.
감사를 외부 sink로 보내지 않는 서비스라면 `platform.governance.operational.require-audit-sink-in-production=false`를 명시한다.

## External AuditLogRecorder migration

외부 `AuditLogRecorder` bean은 기본적으로 무시된다.
`AuditSink` bean으로 옮겨야 한다. mainline starter는 `AuditLogRecorder` fan-out compat를 제공하지 않는다.

## GovernancePolicyService override 실패

`GovernancePolicyService`는 공식 override point가 아니다.
서비스가 이 bean을 직접 등록하면 시작이 실패한다.
정책 판단은 `GovernanceDecisionEngine` 또는 `GovernancePolicyPlugin`으로 교체하고, audit/violation/config는 `AuditSink`, `ViolationHandler`, `PolicyConfigSource`로 확장한다.

## Policy config 누락

운영 profile에서 `violation.action=DENY` 또는 `ESCALATE`인데 policy config source가 비운영 상태이면 기본적으로 시작에 실패한다.
source가 snapshot을 지원하고 snapshot이 비어 있어도 기본적으로 시작에 실패한다.
lazy/remote source처럼 snapshot 없이 key resolve가 가능한 구현은 `supportsSnapshot=false`, `operationalStatus=OPERATIONAL`로 capability를 분리한다.
정책을 코드 plugin만으로 판단하는 서비스라면 `platform.governance.operational.require-policy-config-in-enforcing-mode=false`를 명시한다.
identity audit 중심 서비스라면 `service-role-preset=identity-service`를 우선 검토한다.

## Plugin file store 경로 누락

`platform.governance.feature-flags.store=FILE`이면 `platform.governance.feature-flags.file-path`가 필요하다.
기존 `platform.governance.plugin-policy-engine.*` prefix는 2.0.1 deprecated alias이며 3.0.0에서 제거한다.
새 `platform.governance.feature-flags.*` prefix와 legacy prefix를 동시에 쓰면 profile과 무관하게 시작에 실패한다.
이 검증은 profile과 무관하게 적용되며 `platform.governance.operational.fail-fast-enabled=false`로도 우회되지 않는다.

## Violation handler 실패

기본값은 handler 실패를 audit에 남기고 verdict 반환을 유지한다.
운영 profile에서는 `violation.handler-failure-fatal=true`가 기본 요구사항이다.

## AUDIT_ONLY 또는 ALERT인데 DENY 반환

정상 동작이다.
`GovernanceVerdict`가 최종 평가 결과를 결정하고, `ViolationAction`은 `DENY` verdict 이후의 대응 방식을 정한다.
`AUDIT_ONLY`와 `ALERT`는 verdict를 `ALLOW`로 바꾸지 않는다.
