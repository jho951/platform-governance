# platform-governance

`platform-governance`는 `audit-log`, `policy-config`, `plugin-policy-engine`를 조립하는 2계층 governance platform이다.
내부 구현은 1계층 OSS의 Maven Central 배포본을 소비하는 기준으로 유지한다.

## 역할

- 구조화 감사
- 정책 설정 조회
- 정책 평가 엔진

## 모듈

- `platform-governance-api`
- `platform-governance-bom`
- `platform-governance-audit`
- `platform-governance-config`
- `platform-governance-core`
- `platform-governance-engine`
- `platform-governance-spring`
- `platform-governance-spring-boot-starter`
- `platform-governance-common-test`

## 핵심 원칙

- 1계층 OSS의 Maven Central 배포본을 조합한다.
- 2계층 `platform-governance`는 GitHub Packages private package로 배포한다.
- `service-contract`와 호환되는 거버넌스 경계만 제공한다.
- platform 내부에서 1계층 상세 구현을 다시 정의하지 않는다.

## 소비 모듈 기준

- `audit-log` 계열은 `io.github.jho951:audit-log-api:2.0.0`, `io.github.jho951:audit-log-core:2.0.0` 배포 묶음을 기준으로 사용한다.
- `policy-config` 계열은 `io.github.jho951:policy-config-contracts:2.0.0`, `io.github.jho951:policy-config-core:2.0.0`, `io.github.jho951:policy-config-builder:2.0.0` 배포 묶음을 기준으로 사용한다.
- `plugin-policy-engine` 계열은 `io.github.jho951:plugin-policy-engine-api:2.0.1`, `io.github.jho951:plugin-policy-engine-config:2.0.1`, `io.github.jho951:plugin-policy-engine-core:2.0.1` 배포 묶음을 기준으로 사용한다.

## 빌드

```bash
./gradlew test
```

## Private 소비

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-governance-bom:1.0.1")
    implementation "io.github.jho951.platform:platform-governance-spring-boot-starter"
}
```

로컬 1계층 소스로 사전 검증할 때만 다음 옵션을 사용한다.

```bash
./gradlew test -PuseLocalLayer1=true
```

## [문서](docs/README.md)