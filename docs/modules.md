# Modules

## 역할

- `platform-governance-api`: 요청, context, verdict, audit, config 계약
- `platform-governance-bom`: private consumer용 dependency constraints
- `platform-governance-audit`: `audit-log` 기반 audit recorder adapter
- `platform-governance-config`: `policy-config` 기반 config source adapter
- `platform-governance-core`: platform 공통 정책 서비스
- `platform-governance-engine`: `plugin-policy-engine` 기반 평가 엔진
- `platform-governance-spring`: Spring boot auto-configuration
- `platform-governance-spring-boot-starter`: starter 진입점
- `platform-governance-common-test`: 테스트 픽스처와 샘플

## 조합 규칙

- core는 순수 Java 로직만 담는다.
- bom은 2계층 platform 모듈과 1계층 OSS exact version을 고정한다.
- audit/config/engine은 1계층 OSS별 adapter와 조립 로직을 분리해 담는다.
- spring은 빈 등록만 담당한다.
- starter는 Spring 의존성을 모은다.
- common-test는 테스트 데이터와 fixture를 제공한다.
- 1계층 기능은 `project()`가 아니라 Maven Central published artifact로 소비한다.
- 2계층 모듈은 GitHub Packages private Maven registry로 배포한다.
