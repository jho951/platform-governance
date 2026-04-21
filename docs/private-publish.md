# Private Publish And Consumption

`platform-governance`는 private GitHub Packages로 배포한다.
`platform-governance`는 내부 서비스용 private package다. 외부 audit/config/engine library는 내부 모듈이 소비하고, 3계층은 starter를 소비한다.

## Publish 대상

배포 대상:

- `platform-governance-bom`
- `platform-governance-api`
- `platform-governance-audit`
- `platform-governance-config`
- `platform-governance-core`
- `platform-governance-engine`
- `platform-governance-spring`
- `platform-governance-starter`
- `platform-governance-common-test`

배포 제외:

- `platform-governance-samples`

소비 서비스는 `platform-governance-starter`를 공식 진입점으로 사용한다.

## GitHub Actions publish

publish workflow는 `v*` tag push 또는 수동 dispatch로 실행된다.

```bash
git tag v2.0.2
git push origin v2.0.2
```

workflow는 tag에서 version을 계산한다.

```text
v2.0.2 -> platformReleaseVersion=2.0.2
```

필수 workflow 권한:

```yaml
permissions:
  contents: read
  packages: write
```

Actions에서는 아래 값이 자동 제공된다.

```text
GITHUB_ACTOR=${{ github.actor }}
GITHUB_TOKEN=${{ secrets.GITHUB_TOKEN }}
```

현재 publish command 형태:

```bash
./gradlew clean test publish \
  -PplatformReleaseVersion="${VERSION}" \
  -PgithubPackagesUrl="https://maven.pkg.github.com/jho951/platform-governance" \
  -PgithubPackagesUsername="${GITHUB_ACTOR}" \
  -PgithubPackagesToken="${GITHUB_TOKEN}"
```

## 로컬 publish

로컬에서 publish해야 하면 PAT가 필요하다.

```bash
export GITHUB_ACTOR=jho951
export GITHUB_TOKEN=<write:packages 권한이 있는 PAT>

./gradlew clean test publish \
  -PplatformReleaseVersion=2.0.2 \
  -PgithubPackagesUrl=https://maven.pkg.github.com/jho951/platform-governance \
  -PgithubPackagesUsername="$GITHUB_ACTOR" \
  -PgithubPackagesToken="$GITHUB_TOKEN"
```

권장 PAT 권한:

- `write:packages`
- `read:packages`
- private repo 접근이 필요한 경우 `repo`

## Consumer 설정

private package를 소비하는 서비스는 GitHub Packages repository와 credential이 필요하다.

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/jho951/platform-governance")
        credentials {
            username = providers.gradleProperty("githubPackagesUsername")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .getOrNull()
            password = providers.gradleProperty("githubPackagesToken")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .getOrNull()
        }
    }
}
```

dependency:

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-governance-bom:2.0.2")
    implementation "io.github.jho951.platform:platform-governance-starter"
}
```

## Consumer CI secrets

소비 서비스 repo에는 PAT secret을 두는 편이 안전하다.

권장 이름:

```text
GH_PACKAGES_TOKEN
```

workflow:

```yaml
env:
  GITHUB_ACTOR: ${{ github.actor }}
  GITHUB_TOKEN: ${{ secrets.GH_PACKAGES_TOKEN }}
```

cross-repo private package consumption은 기본 `secrets.GITHUB_TOKEN`으로 실패할 수 있다.
그 경우 `read:packages`와 repo 접근 권한이 있는 PAT를 사용한다.

## 자주 나는 오류

### 401 Unauthorized

원인:

- `GITHUB_TOKEN`이 비어 있음
- PAT에 `read:packages`가 없음
- private repo/package 접근 권한이 없음

### 403 Forbidden

원인:

- token은 있지만 package 권한이 부족함
- 다른 repo에서 private package를 읽는데 PAT에 `repo` 권한이 없음

### Could not find artifact

원인:

- version이 publish되지 않음
- GitHub Packages repository URL이 틀림
- BOM version과 starter version이 다름
