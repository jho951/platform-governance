# Dependency Basis

`platform-governance`는 외부 audit/config/engine library의 Maven Central 배포본만 소비한다.
3계층 서비스는 이 library를 직접 조립하지 않고, 2계층 `platform-governance`가 제공하는 실행 골격과 override 지점만 소비한다.

## 금지

- `project(':...')` 형태의 로컬 소스 레포 직접 의존
- 외부 library의 내부 모듈 구조를 그대로 복사하는 방식
- platform 내부에서 외부 library 책임을 다시 구현하는 방식
- 3계층 서비스가 외부 library adapter, builder, engine을 직접 조립하는 방식

## 기준 좌표

- `audit-log`: `io.github.jho951:audit-log-api:2.0.0`, `io.github.jho951:audit-log-core:2.0.0`
- `policy-config`: `io.github.jho951:policy-config-contracts:2.0.0`, `io.github.jho951:policy-config-core:2.0.0`, `io.github.jho951:policy-config-builder:2.0.0`
- `plugin-policy-engine`: `io.github.jho951:plugin-policy-engine-config:2.0.1`

## 적용 원칙

- `audit`, `config`, `engine`은 adapter와 조립 로직을 분리해 가진다. 현재 engine은 platform `GovernancePolicyPlugin` chain이고, 외부 `plugin-policy-engine` API adapter는 아니다.
- `core`는 특정 외부 library에 직접 묶이지 않는 공통 정책 서비스만 가진다.
- `spring`은 외부 config/builder 컴포넌트와 platform capability module을 조합해 bean을 구성한다.
- `starter`는 platform 소비자가 의존하는 진입점이다.
- 2계층은 공통 운영정책의 실행 골격을 제공하고, 정말 전사 공통인 불변 규칙만 강제한다.
- 3계층은 자기 서비스에 필요한 정책 값만 선언하며, 서비스 차이가 필요한 정책은 override로 조정한다.
- 로컬 외부 library 소스 검증은 `-PuseLocalLayer1=true`를 명시한 경우에만 허용한다.
