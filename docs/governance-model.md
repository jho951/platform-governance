# Governance Model

`platform-governance`는 요청 단위로 audit, config resolution, policy engine을 평가한다.

## 평가 순서

1. 정책 설정을 조회한다.
2. 플러그인 엔진이 정책을 평가한다.
3. 감사 이벤트를 기록한다.
4. 모두 통과하면 allow

## 결과

- `ALLOW`
- `DENY`

## 기준

- 정책 소스가 없으면 기본 값으로 동작한다.
- 감사 기록은 정책 결과를 설명할 수 있어야 한다.
- 플러그인 엔진은 결정 사유를 반환해야 한다.

