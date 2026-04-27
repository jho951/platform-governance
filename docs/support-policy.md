# Support Policy

`platform-governance`는 `release_version` 기준으로 versioned publish를 수행한다.

## Versioning

- public governance SPI 변경은 release note와 docs를 먼저 갱신한다.
- breaking change는 migration note와 함께 배포한다.
- common-test artifact도 같은 release train version을 따른다.

## Verification

배포 후보는 최소 다음을 통과해야 한다.

```bash
./gradlew clean check
```

## CI workflow

- `build.yml`은 `./gradlew clean check`를 stage-5 entry gate로 사용한다.
- `publish.yml`은 versioned publish 전에 같은 gate를 다시 통과시킨다.

## Support scope

- 공식 지원 surface는 starter, public API, `GovernanceAuditSink`, `PolicyConfigSource`, `GovernanceDecisionEngine`, official common-test artifact다.
- raw audit-log/policy-config implementation detail은 support contract가 아니다.
