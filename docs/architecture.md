# Architecture

`platform-governance`는 1계층 governance OSS를 조립해 서비스 서버가 소비하는 거버넌스 기반을 제공한다.
실제 구현은 Maven Central에 배포된 1계층 OSS를 기준으로 조립하며, 3계층 서비스 서버가 1계층 OSS를 직접 조립하지 않게 하는 2계층 실행 골격이다.
범위는 감사, 정책 조회, 정책 평가, 정책 변경 기록, 위반 대응을 위한 API/SPI, runtime 조립, 운영 guard다.

이 문서의 계층은 전통적인 n-tier가 아니라 `primitive / platform runtime / deployable service` 분류다. `platform-governance`는 플랫폼 성격을 가지지만 2계층 runtime이다. 자체 endpoint, 자체 DB, 운영자용 publish/revoke/history workflow의 최종 owner가 되면 그 컴포넌트는 별도 3계층 서비스로 분류한다.

## 계층

- `platform-governance-api`: 외부 계약
- `platform-governance-bom`: private consumer용 버전 정렬
- `platform-governance-audit`: audit adapter
- `platform-governance-config`: config adapter
- `platform-governance-core`: 공통 정책 서비스
- `platform-governance-engine`: platform `GovernancePolicyPlugin` chain 기반 평가 엔진
- `platform-governance-spring`: Spring 자동 구성
- `platform-governance-spring-boot-starter`: 진입점
- `platform-governance-common-test`: 테스트 픽스처

## 조립 대상

- `audit-log`
- `policy-config`
- `plugin-policy-engine-config`

## 책임 경계

2계층 `platform-governance`가 담당한다.

- audit-log, policy-config, plugin-policy-engine-config 조립
- 공통 governance 요청/결과 모델 제공
- 정책 설정 조회 표준화
- 정책 평가 실행 골격 제공
- 감사 이벤트 기록 표준화
- identity audit taxonomy와 필수 필드 검증 제공
- 정책 변경 기록 API/SPI와 adapter 표준화
- 위반 발생 시 handler 실행
- Spring Boot auto-configuration/starter 제공
- 운영에서 위험한 설정 fail-fast 차단
- 서비스 역할 preset 기반 운영 기본값 보강
- 서비스별 차이는 SPI/bean override로 개방
- 자체 controller, 자체 DB, 운영자 workflow의 최종 소유권은 갖지 않음

3계층 서비스가 담당한다.

- 특정 서비스의 승인/반려 업무 규칙
- 특정 도메인 리소스 권한 판단
- 서비스 정책 key와 정책 값 정의
- 특정 조직/tenant 정책
- 실제 정책 저장소의 도메인 스키마
- 서비스 이벤트 이름과 도메인 이벤트 매핑
- 정책 publish/revoke/history가 업무 의미를 가지면 해당 workflow의 최종 소유권
- 별도 `Governance-server`가 생기는 경우 그 서버의 endpoint, DB, 운영자 관리 기능

Auth-server 같은 identity 서비스는 `IdentityAuditRecorder`로 표준 action을 호출하고, 2계층이 이를 1계층 `audit-log`의 `AuditLogger` 이벤트로 변환한다.

## 서비스 역할 preset

`service-role-preset`은 governance 운영 기본값을 서비스 주 역할에 맞춰 보강한다.

| Preset | 기본 방향 |
| --- | --- |
| `IDENTITY_SERVICE` | identity audit validation과 fail-closed audit을 유지하고, 정책 config가 없어도 audit 중심 서비스로 운영 가능 |
| `POLICY_DECISION_SERVICE` | strict engine, DENY violation, fatal handler failure를 기본으로 적용 |
| `RESOURCE_SERVICE` | resource API의 정책 평가를 강하게 적용 |
| `OBSERVABILITY_SERVICE` | audit/alert 중심 운영을 허용하되 service identity와 audit sink는 유지 |

Preset은 서비스 이름을 알지 않는다. `auth-server`, `authz-server`, `block-server` 같은 이름별 분기는 3계층 설정으로 남긴다.

## 원칙

- platform은 private 레포다.
- 3계층 서비스는 1계층 OSS를 직접 조립하지 않는다.
- 2계층 platform은 1계층 OSS의 Maven Central 배포본을 기준으로 공통 운영정책의 실행 골격을 제공한다.
- 3계층 서비스는 자기 서비스에 필요한 정책 값만 선언한다.
- 정말 전사 공통인 불변 규칙만 2계층에서 강제한다.
- 서비스 차이가 필요한 부분은 override 가능하게 둔다.
- 정책 변경과 위반 대응은 2계층 표준 계약과 기본 구현으로 연결하되, 상태와 운영 workflow의 최종 owner는 3계층에 둔다.
- role preset은 명시 설정을 덮어쓰지 않고, 비어 있는 운영 기본값만 채운다.
- 2계층 artifact는 GitHub Packages private Maven registry로 배포한다.
- 1계층 OSS의 의미를 다시 정의하지 않는다.
- 2계층은 범용 `AuditSink`/delivery primitive를 재구현하지 않고 1계층 `audit-log`를 소비한다.
- 서비스 서버는 `service-contract`를 기준으로 이 platform을 소비한다.
- security와 storage 플랫폼과 직접 구현 의존을 만들지 않는다.
- `platform-owned`와 `2계층`은 같은 말이 아니다. 플랫폼 소유 서비스라도 deploy되고 상태/workflow를 소유하면 3계층이다.

## 현재 구현

- 구조화 감사 이벤트
- identity audit 표준 API와 taxonomy
- 정책 설정 소스
- 플러그인 기반 정책 평가 엔진
- 감사 포함 거버넌스 평가
- 정책 변경 기록
- 위반 대응 handler 실행
- 운영 profile 위험 설정 fail-fast 검증
