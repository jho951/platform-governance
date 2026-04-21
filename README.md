# platform-governance

`platform-governance`는 `audit-log`, `policy-config`, platform `GovernancePolicyPlugin` chain, feature flag config 기준을 조립해 감사, 정책 조회, 정책 평가, 정책 변경 기록, 위반 대응을 표준화하는 2계층 운영 통제 계층이다.
내부 구현은 audit/config/engine 의존성 경계로 나누되, 서비스 서버인 3계층은 단일 starter와 설정만 소비하도록 한다.

## 역할

- 구조화 감사
- identity audit 표준 API와 이벤트 taxonomy
- 정책 설정 조회
- 정책 평가 엔진
- 정책 변경 기록
- 위반 대응 실행
- 운영 위험 설정 fail-fast 검증
- service-role-preset 기반 운영 기본값 보강

`auth-service` 같은 3계층 identity 서비스는 범용 audit entry를 직접 만들지 않고 `IdentityAuditRecorder`로 사건만 알린다.
`platform-governance`는 schema, required field validation, correlation, redaction, sink delivery 위임, 운영 fail-fast를 처리한다.

## 3계층 사용 방식

Spring Boot 3계층 서비스는 BOM과 단일 starter를 기본 진입점으로 사용한다.

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-governance-bom:2.0.2")
    implementation "io.github.jho951.platform:platform-governance-starter"
}
```

서비스별 차이는 artifact가 아니라 `platform.governance.service-role-preset`과 공식 확장 bean으로 표현한다.

3계층 서비스는 `platform.governance.service-role-preset`으로 governance 주 역할을 선언할 수 있다.

| Preset | 주 대상 |
| --- | --- |
| `IDENTITY_SERVICE` | auth-service |
| `POLICY_DECISION_SERVICE` | authz-service |
| `RESOURCE_SERVICE` | user-service, editor-service |
| `OBSERVABILITY_SERVICE` | monitoring-service |
| `GENERAL` | 직접 설정 |

Preset은 사용자가 명시한 설정을 덮어쓰지 않고, 비어 있는 운영 기본값만 보강한다.

## 제공하지 않는 것

- 특정 서비스의 승인/반려 업무 규칙
- 특정 도메인 리소스 권한 판단
- 특정 서비스 정책 key 하드코딩
- 특정 DB/Redis key 규칙 강제
- 특정 서비스 이벤트 이름 강제
- audit sink delivery primitive 재구현
- 외부 audit/config/engine library 의미 재정의

## 모듈

- `platform-governance-api`
- `platform-governance-bom`
- `platform-governance-audit`
- `platform-governance-config`
- `platform-governance-core` - Spring/audit wrapper 없는 pure Java reference engine
- `platform-governance-engine`
- `platform-governance-spring`
- `platform-governance-starter` - 3계층 공식 Spring Boot 소비 진입점
- `platform-governance-common-test`
- `platform-governance-samples` - 배포 제외 smoke test/소비 예제

## 핵심 원칙

- 3계층 서비스는 audit/config/engine library를 직접 조립하지 않는다.
- 2계층 `platform-governance`는 공통 운영정책의 실행 골격을 제공한다.
- 3계층 서비스는 자기 서비스에 필요한 정책 값만 선언한다.
- 정말 전사 공통인 불변 규칙만 2계층에서 강제한다.
- 서비스 차이가 필요한 부분은 override 가능하게 둔다.
- 정책 변경과 위반 대응은 2계층 공개 API로 처리한다.
- 2계층 `platform-governance`는 GitHub Packages private package로 배포한다.
- `service-contract`와 호환되는 거버넌스 경계만 제공한다.

## 소비 모듈 기준

- `audit-log` 계열은 `io.github.jho951:audit-log-api:2.0.0`, `io.github.jho951:audit-log-core:2.0.0` 배포 묶음을 기준으로 사용한다.
- `policy-config` 계열은 `io.github.jho951:policy-config-contracts:2.0.0`, `io.github.jho951:policy-config-core:2.0.0`, `io.github.jho951:policy-config-builder:2.0.0` 배포 묶음을 기준으로 사용한다.
- `plugin-policy-engine` 계열은 feature flag 설정 bean 제공에 필요한 `io.github.jho951:plugin-policy-engine-config:2.0.1` 배포본을 기준으로 사용한다.

## plugin-policy-engine 관계

현재 `platform-governance`의 실제 정책 실행 엔진은 `platform-governance-engine`의 `GovernancePolicyPlugin` chain이다.
`plugin-policy-engine` 전체 runtime을 흡수하거나 외부 engine API adapter로 재포장하지 않는다.
`plugin-policy-engine` 계열은 feature flag/config 호환 기준과 BOM 정렬 대상으로만 둔다.
설정 prefix는 `platform.governance.feature-flags.*`를 사용한다. 기존 `platform.governance.plugin-policy-engine.*`는 2.0.1 deprecated alias이며 3.0.0에서 제거한다. 두 prefix를 동시에 설정하면 시작에 실패한다.

`platform-governance-audit`는 ready-made `LoggingAuditSink`를 제공한다.
`AuditSink` bean이 없으면 starter는 governance audit을 애플리케이션 로그로 남겨 local/dev 환경에서 감사가 조용히 사라지지 않게 한다.
외부 `AuditLogRecorder` fan-out은 기본 비활성이며 `platform.governance.compat.audit-log-recorder-fanout-enabled=true`일 때만 임시 migration 경로로 허용한다.
`GovernancePolicyService` 직접 override는 지원하지 않으며, 서비스는 `GovernanceDecisionEngine`, `PolicyConfigSource`, `ViolationHandler`, `AuditSink`를 사용해 확장한다.

## 빌드

```bash
./gradlew test
```

공식 BOM/starter 조합 smoke test:

```bash
./gradlew :platform-governance-samples:test
```

## Private 소비

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-governance-bom:2.0.2")
    implementation "io.github.jho951.platform:platform-governance-starter"
}
```

예:

```yaml
platform:
  governance:
    service-role-preset: identity-service
    audit:
      service-name: auth-service
      environment: prod
```

로컬 외부 라이브러리 소스로 사전 검증할 때만 다음 옵션을 사용한다.

```bash
./gradlew test -PuseLocalLayer1=true
```

## [문서](docs/README.md)
