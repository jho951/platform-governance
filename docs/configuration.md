# Configuration

`platform-governance`는 `platform.governance.*` prefix로 설정한다.

## 기본 속성

- `enabled`
- `audit.enabled`
- `policy-config.values.*`
- `plugin-policy-engine.store`
- `plugin-policy-engine.file-path`
- `plugin-policy-engine.cache-ttl-millis`
- `engine.strict`

## 기본 동작

- audit는 기본 활성화
- audit logger는 `audit-log-core`의 기본 구현을 사용하고, 등록된 `AuditSink`가 있으면 fan-out 한다.
- policy config는 `policy-config`의 `PolicyResolver` 기반 기본 소스를 제공한다.
- plugin policy engine은 `plugin-policy-engine`의 `FeatureFlagClient`를 기본 제공한다.
- engine은 기본적으로 모든 plugin을 순회

## 설정 예시

```yaml
platform:
  governance:
    audit:
      enabled: true
    policy-config:
      values:
        feature.review.required: "true"
    plugin-policy-engine:
      store: memory
      cache-ttl-millis: 3000
    engine:
      strict: false
```
