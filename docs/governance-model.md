# Governance Model

`platform-governance`는 요청 단위로 audit, config resolution, policy engine, violation handling을 평가한다.
정책 변경은 `PolicyChangeRecorder` 표준 계약으로 기록한다.
identity 운영 증적은 `IdentityAuditRecorder` 표준 계약으로 기록하고, 내부 구현은 audit library로 위임한다.

## 평가 순서

1. `GovernanceRequest`를 생성한다.
2. `GovernanceDecisionEngine`이 `GovernancePolicyPlugin` 목록을 순회해 정책을 평가한다.
3. 내부 audit pipeline이 요청, 컨텍스트, verdict, 정책 근거를 기록한다.
5. `DENY` verdict가 나오면 `GovernanceViolation`을 생성한다.
6. 등록된 `ViolationHandler`가 위반 대응을 실행한다.
7. `GovernanceVerdict`를 반환한다.

## 계약 모델

- `GovernanceRequest`: subject, resource, action, attributes, occurredAt
- `GovernanceContext`: actor, environment, attributes
- `GovernanceVerdict`: ALLOW/DENY, policy, reason, evidence
- `PolicyEvidence`: policyId, policyVersion, decision, ruleId, violationCode
- `GovernanceViolation`: request, context, verdict, action, attributes, occurredAt
- `PolicyChangeEvent`: actor, policyKey, previousValue, newValue, reason, attributes, occurredAt
- `IdentityAuditEvent`: action, actor, target, result, reason, severity, correlation, policyEvidence, channel/provider/loginId, attributes, occurredAt

## Audit pipeline

내부적으로 governance audit 기록은 `AuditLogRecorder` adapter를 통해 audit library로 전달한다.
외부 소비자가 production 감사 출력 대상을 추가할 때의 공식 SPI는 `AuditSink`다.
외부 `AuditLogRecorder` fan-out은 2.x 호환 경로로만 유지하며 2.0.1부터 deprecated, 3.0.0 제거 대상이다.

## Identity audit taxonomy

- `IDENTITY_LOGIN_SUCCEEDED`
- `IDENTITY_LOGIN_FAILED`
- `IDENTITY_LOGOUT`
- `IDENTITY_TOKEN_ISSUED`
- `IDENTITY_TOKEN_REFRESHED`
- `IDENTITY_TOKEN_REVOKED`
- `IDENTITY_TOKEN_REUSE_DETECTED`
- `IDENTITY_SESSION_CREATED`
- `IDENTITY_SESSION_FORCE_LOGOUT`
- `IDENTITY_CREDENTIAL_RESET`
- `IDENTITY_ADMIN_SESSION_TERMINATED`
- `IDENTITY_SIGNING_KEY_ROTATED`
- `IDENTITY_INTERNAL_CALLER_DENIED`
- `IDENTITY_ADMIN_IP_DENIED`

Auth-server는 서비스 로컬 이벤트명 대신 위 표준 action을 사용하고, 차이는 `channel`, `provider`, `loginId`, 추가 attributes로 표현한다.

## 결과

- `ALLOW`
- `DENY`

## 위반 대응

- `GovernanceVerdict`가 최종 `ALLOW`/`DENY` 평가 결과를 결정한다.
- `ViolationAction`은 `DENY` verdict가 나온 뒤 실행할 위반 대응 방식이다.
- `AUDIT_ONLY`와 `ALERT`는 운영 위반 대응으로는 약한 action이다.
- `DENY`와 `ESCALATE`는 거부 verdict에 맞춘 강한 대응 action이다.
- `ESCALATE`는 거부 verdict와 함께 상위 대응 handler를 실행할 때 사용한다.

## 기준

- 정책 소스가 없으면 기본 값으로 동작한다.
- 감사 기록은 정책 결과를 설명할 수 있어야 한다.
- 감사 기록은 정책 source 전체 snapshot이 아니라 결정에 필요한 요청/컨텍스트/근거만 남긴다.
- identity 감사는 action별 필수 actor, target, reason, correlation을 검증해야 한다.
- 감사 attributes에는 password, raw token, authorization code, secret, cookie를 남기지 않는다.
- 플러그인 엔진은 결정 사유와 plugin failure 의미를 반환해야 한다.
- 정책 변경은 actor, policy key, 이전 값, 새 값, 사유를 기록할 수 있어야 한다.
- 위반 대응은 action, policy, reason, request, context를 기록할 수 있어야 한다.
