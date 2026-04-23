# Modules

## 역할

- `platform-governance-api`: 요청, context, verdict, identity audit, config 계약
- `platform-governance-bom`: private consumer용 dependency constraints
- `platform-governance-adapter-auditlog`: `audit-log` 기반 audit recorder adapter. `AuditLogRecorder` 내부 seam도 이 artifact에만 존재한다.
- `platform-governance-adapter-policyconfig`: `policy-config` 기반 config source adapter
- `platform-governance-core`: Spring/audit/violation wrapper가 없는 pure Java reference engine
- `platform-governance-engine`: platform `GovernancePolicyPlugin` chain 기반 평가 엔진
- `platform-governance-autoconfigure`: Spring boot auto-configuration과 운영 fail-fast 검증
- `platform-governance-starter`: 3계층 공식 Spring Boot starter 진입점
- `platform-governance-common-test`: 테스트 픽스처
- `platform-governance-samples`: 공식 BOM/starter 조합 검증 샘플

`platform-governance-autoconfigure`는 `service-role-preset`도 적용한다. preset은 3계층 서비스 이름을 보지 않고, identity/policy/resource/observability 같은 주 역할만 사용한다.

## 조합 규칙

- core는 순수 Java 로직만 담는다.
- bom은 2계층 platform 모듈과 외부 audit/config/engine library version을 고정한다.
- audit/config/engine은 의존성별 adapter와 조립 로직을 분리해 담는다.
- identity audit API는 2계층에서 제공하고, sink/delivery는 외부 audit library에 위임한다.
- spring은 빈 등록만 담당한다.
- starter는 공식 소비 좌표이며 Spring Boot auto-configuration 의존성을 모은다.
- common-test는 테스트 데이터와 fixture를 제공한다.
- samples는 배포하지 않는 소비 예제와 smoke test를 제공한다.
- 외부 기능은 `project()`가 아니라 Maven Central published artifact로 소비한다.
- 3계층 서비스는 starter와 설정 값만 소비하며 외부 audit/config/engine library를 직접 조립하지 않는다.
- stage-5 compile classpath 기준에서 starter는 adapter/core/engine 구현을 서비스에 직접 노출하지 않는다.
- 서비스가 `AuditSink`, `AuditLogger`, `AuditEvent`를 코드에서 직접 import하면 `io.github.jho951:audit-log-api`를 명시적으로 추가한다.
- 서비스별 차이가 필요한 부분은 설정 또는 조건부 bean으로 override 가능하게 둔다.
- 정책 변경 기록과 위반 대응은 api 계약과 audit 기반 기본 구현을 통해 제공한다.
- 운영 위험 설정 검증은 spring 모듈에서 수행한다.
- 2계층 모듈은 GitHub Packages private Maven registry로 배포한다.
