# Architecture

`platform-governance`는 1계층 governance OSS를 조립해 서비스 서버가 소비하는 거버넌스 기반을 제공한다.
실제 구현은 Maven Central에 배포된 1계층 OSS를 기준으로 조립한다.

## 계층

- `platform-governance-api`: 외부 계약
- `platform-governance-bom`: private consumer용 버전 정렬
- `platform-governance-audit`: audit adapter
- `platform-governance-config`: config adapter
- `platform-governance-core`: 공통 정책 서비스
- `platform-governance-engine`: plugin 기반 평가 엔진
- `platform-governance-spring`: Spring 자동 구성
- `platform-governance-spring-boot-starter`: 진입점
- `platform-governance-common-test`: 테스트 픽스처

## 조립 대상

- `audit-log`
- `policy-config`
- `plugin-policy-engine`

## 원칙

- platform은 private 레포다.
- 1계층 OSS의 Maven Central 배포본을 기준으로 조립한다.
- 2계층 artifact는 GitHub Packages private Maven registry로 배포한다.
- 1계층 OSS의 의미를 다시 정의하지 않는다.
- 서비스 서버는 `service-contract`를 기준으로 이 platform을 소비한다.
- security와 storage 플랫폼과 직접 구현 의존을 만들지 않는다.

## 현재 구현

- 구조화 감사 이벤트
- 정책 설정 소스
- 플러그인 기반 정책 평가 엔진
- 감사 포함 거버넌스 평가
