# Consumer Conformance

`platform-governance-samples`는 공식 3계층 governance consumer conformance test다.

## 보장해야 하는 것

- 서비스는 `platform-governance-starter`만으로 dev/prod profile에서 부팅할 수 있어야 한다.
- official `GovernanceAuditSink` surface로 governance violation audit이 기록되어야 한다.
- external `GovernanceAuditRecorder` override는 mainline starter contract를 우회하지 못해야 한다.
- 서비스는 service-owned audit bridge 없이 public SPI만으로 governance를 소비해야 한다.
- `PolicyConfigSource`와 `GovernancePolicyPlugin`만으로 sample consumer를 구성할 수 있어야 한다.

## 실행

```bash
./gradlew :platform-governance-samples:test
```

루트 `./gradlew check`는 이 conformance test를 `verifyConsumerConformance`를 통해 기본 gate에 포함한다.

공식 재사용 fixture artifact는 `platform-governance-common-test`다.
