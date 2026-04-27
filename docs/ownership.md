# Ownership

`platform-governance`는 감사, 정책 조회, 정책 평가, 위반 대응, 운영 fail-fast를 조립하는 2계층 governance runtime이다.

## 2계층이 소유하는 것

- `GovernanceAuditSink`, `GovernanceAuditRecorder`
- `PolicyConfigSource`, `GovernanceDecisionEngine`, `ViolationHandler`
- service-role preset과 운영 기본값 보강
- audit/config/engine adapter 조립
- starter, auto-configuration, smoke sample

## 3계층이 소유하는 것

- 서비스 정책 key와 값
- domain-specific `GovernancePolicyPlugin`
- sink bean 선택과 운영 목적지
- 서비스 identity와 domain context attribute
- business workflow 의미와 승인/반려 규칙

## Stage-5 규칙

- 서비스는 `platform-governance-starter`를 기본 진입점으로 사용한다.
- raw `AuditSink`, `AuditEvent`, `policy-config` 내부 타입을 서비스 compile surface에 직접 노출하지 않는다.
- 서비스 확장은 `GovernanceAuditSink`, `GovernanceAuditRecorder`, `PolicyConfigSource`, `GovernanceDecisionEngine`, `ViolationHandler`로 한정한다.
- service-owned audit bridge나 config compatibility adapter를 따로 두지 않는다.
- governance sink/source 운영 선택은 bean/config로 표현하고, core engine을 서비스가 다시 조립하지 않는다.

## 두지 않을 것

- service-owned governance audit bridge
- raw audit-log 타입을 쓰는 controller/service layer
- `policy-config` 내부 구현을 직접 묶는 service-local starter
- starter 밖에서 돌아가는 별도 governance recorder override 규약

## 공식 확장 표면

- `GovernanceAuditSink`
- `GovernanceAuditRecorder`
- `PolicyConfigSource`
- `GovernanceDecisionEngine`
- `ViolationHandler`
- `GovernancePolicyPlugin`

## 품질 기준

기본 검증은 `./gradlew check`다.
이 명령은 published surface 검증, starter contract 검증, governance smoke sample을 함께 실행한다.
