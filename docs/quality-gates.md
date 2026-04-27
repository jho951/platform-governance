# Quality Gates

CI는 `./gradlew check`를 기준 gate로 사용한다. 이 명령은 published surface 검증, starter contract 검증, governance smoke sample까지 포함한다.

## 포함 항목

- Java compile lint: `-Xlint:all`, `-Xlint:-serial`
- Checkstyle: import hygiene
- PMD: empty catch, unused local/private members, null collection return rule
- JaCoCo coverage verification
- Dependency locking
- Dependency verification
- `verifyPublishedSurface`, `verifyStarterContract`, `verifyStage5Contract`

## JaCoCo

현재 baseline은 `jacocoMinimumCoverage=0.04`다.
API 모듈의 identity/audit 계약 표면이 크고 테스트가 아직 얕기 때문에 낮게 시작한다.

기준을 올릴 때:

```bash
./gradlew check -PjacocoMinimumCoverage=0.10
```

통과하면 `build.gradle`의 기본값을 올린다.

## Static Analysis

Rule set:

- `config/checkstyle/checkstyle.xml`
- `config/pmd/ruleset.xml`

로컬 확인:

```bash
./gradlew checkstyleMain checkstyleTest pmdMain pmdTest
```

## Dependency Locking

의존성 버전은 각 module의 `gradle.lockfile`에 고정한다.

의존성을 추가하거나 버전을 바꾼 뒤 lock file 갱신:

```bash
./gradlew --write-locks check
```

## Dependency Verification

외부 artifact checksum은 `gradle/verification-metadata.xml`에 기록한다.

의존성 변경 후 metadata 갱신:

```bash
./gradlew --write-verification-metadata sha256 check
```

갱신 후 일반 검증:

```bash
./gradlew check
```
