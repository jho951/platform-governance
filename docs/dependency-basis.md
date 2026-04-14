# Dependency Basis

`platform-governance`는 1계층 OSS의 Maven Central 배포본만 소비한다.

## 금지

- `project(':...')` 형태의 로컬 소스 레포 직접 의존
- 1계층 레포의 내부 모듈 구조를 그대로 복사하는 방식
- platform 내부에서 1계층 책임을 다시 구현하는 방식

## 기준 좌표

- `audit-log`: `io.github.jho951:audit-log-api:2.0.0`, `io.github.jho951:audit-log-core:2.0.0`
- `policy-config`: `io.github.jho951:policy-config-contracts:2.0.0`, `io.github.jho951:policy-config-core:2.0.0`, `io.github.jho951:policy-config-builder:2.0.0`
- `plugin-policy-engine`: `io.github.jho951:plugin-policy-engine-api:2.0.1`, `io.github.jho951:plugin-policy-engine-config:2.0.1`, `io.github.jho951:plugin-policy-engine-core:2.0.1`

## 적용 원칙

- `audit`, `config`, `engine`은 각 1계층 OSS adapter와 조립 로직을 분리해 가진다.
- `core`는 특정 1계층 OSS에 직접 묶이지 않는 공통 정책 서비스만 가진다.
- `spring`은 1계층 config/builder 컴포넌트와 platform capability module을 조합해 bean을 구성한다.
- `starter`는 platform 소비자가 의존하는 진입점이다.
- 로컬 1계층 소스 검증은 `-PuseLocalLayer1=true`를 명시한 경우에만 허용한다.
