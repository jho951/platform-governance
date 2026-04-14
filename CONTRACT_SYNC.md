# CONTRACT_SYNC - platform-governance

## 기준

- `oss-contract`: 2계층 platform 표준
- `service-contract`: 실제 서비스 서버 계약 SOT

## 반영 대상

- `README.md`
- `docs/README.md`
- `docs/architecture.md`
- `docs/modules.md`
- `docs/governance-model.md`
- `docs/configuration.md`
- `docs/dependency-basis.md`
- `docs/extension-guide.md`
- `docs/private-publish.md`
- `build.gradle`
- `settings.gradle`
- `.github/workflows/build.yml`
- `.github/workflows/publish.yml`

## 전제

- `platform-governance`는 private platform 레포다.
- `audit-log`, `policy-config`, `plugin-policy-engine`의 Maven Central 배포본을 조립한다.
- `platform-governance`는 GitHub Packages private Maven registry로 배포한다.
- 로컬 소스 레포에 대한 `project()` 직접 의존은 두지 않는다.

## 의존성 기준

- `audit-log`: `io.github.jho951:audit-log-api:2.0.0`, `io.github.jho951:audit-log-core:2.0.0`
- `policy-config`: `io.github.jho951:policy-config-contracts:2.0.0`, `io.github.jho951:policy-config-core:2.0.0`, `io.github.jho951:policy-config-builder:2.0.0`
- `plugin-policy-engine`: `io.github.jho951:plugin-policy-engine-api:2.0.1`, `io.github.jho951:plugin-policy-engine-config:2.0.1`, `io.github.jho951:plugin-policy-engine-core:2.0.1`

## Private 배포 기준

- group: `io.github.jho951.platform`
- BOM: `io.github.jho951.platform:platform-governance-bom`
- capability modules: `platform-governance-audit`, `platform-governance-config`, `platform-governance-engine`
- starter: `io.github.jho951.platform:platform-governance-spring-boot-starter`
- GitHub Packages private Maven registry를 사용한다.
- GitHub Actions publish는 `v*` tag 또는 수동 dispatch로 실행한다.
- 현재 배포 준비 version은 `1.0.1`이다.
- private consumer는 `GITHUB_ACTOR`와 `GITHUB_TOKEN` 또는 `GH_PACKAGES_TOKEN` secret으로 GitHub Packages를 인증한다.
