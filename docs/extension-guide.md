# Extension Guide

## 새 policy plugin 추가

1. 정책 평가 엔진 확장은 `platform-governance-engine` 또는 별도 plugin module에 `GovernancePolicyPlugin` 구현을 추가한다.
2. `platform-governance-spring`에서 조건부 빈으로 등록한다.
3. `docs/governance-model.md`에 평가 순서를 추가한다.
4. `docs/modules.md`에 역할을 추가한다.

## 주의점

- capability module에 Spring 의존성을 넣지 않는다.
- `platform-governance-core`에는 audit/config/engine별 1계층 조립 책임을 넣지 않는다.
- plugin은 결정 이유를 설명할 수 있어야 한다.
- audit는 결과와 사유를 남겨야 한다.
