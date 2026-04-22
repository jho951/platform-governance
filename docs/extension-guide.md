# Extension Guide

## 새 policy plugin 추가

1. 정책 평가 엔진 확장은 `platform-governance-engine` 또는 별도 plugin module에 `GovernancePolicyPlugin` 구현을 추가한다.
2. `platform-governance-autoconfigure`에서 조건부 빈으로 등록한다.
3. `docs/governance-model.md`에 평가 순서를 추가한다.
4. `docs/modules.md`에 역할을 추가한다.

## 계층 책임

- 3계층 서비스는 audit/config/engine library를 직접 조립하지 않는다.
- 2계층은 공통 운영정책의 실행 골격을 제공한다.
- 3계층은 자기 서비스에 필요한 정책 값만 선언한다.
- 정말 전사 공통인 불변 규칙만 2계층에서 강제한다.
- 서비스 차이가 필요한 부분은 override 가능하게 둔다.
- 정책 변경과 위반 대응은 2계층 공개 API를 사용한다.

## Override point

3계층 서비스가 교체할 수 있는 bean은 다음이다.

- `PolicyConfigSource`
- `GovernancePolicyPlugin`
- `GovernanceDecisionEngine`
- `AuditSink`는 production 감사 출력 대상의 공식 SPI다.
- `LoggingAuditSink`는 `platform-governance-adapter-auditlog`가 제공하는 ready-made sink다.
- `AuditLogRecorder`는 platform 내부 adapter다. 외부 fan-out은 기본 비활성이며, `platform.governance.compat.audit-log-recorder-fanout-enabled=true`일 때만 2.x 임시 compat 경로로 허용한다. 이 경로는 2.0.1부터 deprecated이며 3.0.0에서 제거한다.
- `IdentityAuditRecorder`
- `IdentityAuditCustomizer`
- `AuditAttributeEnricher`
- `AuditContextResolver`
- `PolicyChangeRecorder`
- `ViolationHandler`
- `FeatureFlagClient`
- `PolicyResolver`
- `Clock`

`platform-governance-autoconfigure`는 위 bean들을 기본 등록하고, platform audit recorder는 `AuditSink`로 delivery를 위임한다.
서비스는 `GovernancePolicyPlugin` 또는 `GovernanceDecisionEngine`을 등록해 도메인별 정책 판단만 바꾼다.
`GovernancePolicyService`는 audit 기록, violation handling, wrapper 수준의 공통 골격을 포함하므로 공식 override point가 아니다.
사용자가 `GovernancePolicyService` bean을 등록하면 startup fail-fast로 막는다.
커스텀 `PolicyConfigSource`는 운영 판단을 boolean으로만 숨기지 말고 `operationalStatus()`로 `OPERATIONAL`, `NOT_CONFIGURED`, `UNAVAILABLE`, `UNKNOWN` 중 하나와 이유를 반환한다.

## Preset 확장 기준

새 `service-role-preset`은 여러 서비스에서 반복되는 운영 기본값이 있을 때만 추가한다.

- 특정 서비스 이름을 preset에 넣지 않는다.
- 도메인 정책 key를 preset에 넣지 않는다.
- 명시 설정을 덮어쓰지 않고, 설정하지 않은 기본값만 채운다.
- fail-fast 완화가 필요한 preset은 왜 완화하는지 문서화한다.

## 주의점

- capability module에 Spring 의존성을 넣지 않는다.
- `platform-governance-core`는 pure Java reference engine으로 유지하고 audit/config/engine별 adapter 조립 책임을 넣지 않는다.
- plugin은 결정 이유를 설명할 수 있어야 한다.
- audit는 결과와 사유를 남겨야 한다.
- governance audit는 정책 source 전체 snapshot이 아니라 request/context/verdict/evidence를 남긴다.
- `auth-service` 같은 identity 서비스는 범용 `AuditLogRecorder` 대신 `IdentityAuditRecorder`를 우선 소비한다.
- 2계층은 identity taxonomy와 required field validation을 제공하되, sink 자체는 외부 audit library의 `AuditSink`를 사용한다.
- 정책 변경은 `PolicyChangeRecorder`로 기록한다.
- 위반 대응은 `ViolationHandler`를 추가하거나 override한다.
- 운영 환경에서 fail-fast를 완화해야 한다면 `platform.governance.operational.*`에 의도를 명시한다.
