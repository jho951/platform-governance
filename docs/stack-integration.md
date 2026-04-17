# Stack Integration

이 문서는 `platform-security`, `platform-governance`, `platform-resource`를 한 3계층 서비스에서 같이 쓸 때의 공식 조합 규칙이다.

## Release Train

세 BOM을 직접 맞추지 말고 stack BOM을 먼저 사용한다.

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-stack-bom:1.0.6")
}
```

`platform-stack-bom:1.0.6` 기준 조합:

| 모듈 | 버전 |
| --- | --- |
| `platform-security` | `1.0.6` |
| `platform-governance` | `1.0.0` |
| `platform-resource` | `1.0.0` |
| Spring Boot | `3.5.13` |
| Spring Framework | `6.2.17` |

운영 profile 기본값은 세 모듈 모두 `prod` 하나다. `production`, `live`는 기본 운영 profile로 해석하지 않는다.

## Request Flow

```text
HTTP ingress
-> platform-security
-> 3계층 controller/use case
-> platform-governance policy/audit
-> platform-resource store/open/delete
-> resource/security governance bridge
-> governance audit sink
```

2계층은 공통 흐름과 runtime skeleton만 가진다. 특정 tenant, workspace, 업무 ACL 최종 판단은 3계층 책임이다.

## Role Matrix

| 3계층 서비스 | security | governance | resource |
| --- | --- | --- | --- |
| `gateway-server` | `EDGE` | `GENERAL` | 보통 미사용 |
| `auth-server` | `ISSUER` | `IDENTITY_SERVICE` | 필요 시 일부 사용 |
| `authz-server` | `RESOURCE_SERVER` 또는 `INTERNAL_SERVICE` | `POLICY_DECISION_SERVICE` | 보통 미사용 |
| `user-server` | `RESOURCE_SERVER` | `RESOURCE_SERVICE` | 사용 |
| `block-server` | `RESOURCE_SERVER` | `RESOURCE_SERVICE` | 사용 |
| `monitor-server` | `INTERNAL_SERVICE` 또는 `RESOURCE_SERVER` | `OBSERVABILITY_SERVICE` | 보통 미사용 |

한 서비스에서는 security 역할별 starter를 하나만 고른다. governance 역할은 `platform.governance.service-role-preset`으로 맞춘다.

## Policy Config

정책 설정 source의 기준 계약은 governance `PolicyConfigSource`다.

```java
@Bean
io.github.jho951.platform.governance.api.PolicyConfigSource policyConfigSource() {
    return new MyPolicyConfigSource();
}
```

`platform-security-policyconfig-bridge`는 governance `PolicyConfigSource`를 감지하면 security IP guard용 policy config source로 자동 adapter를 등록한다. 기존 security `io.github.jho951.platform.policy.api.PolicyConfigSource` bean을 직접 제공하는 방식도 호환된다.

## Audit Fan-Out

security audit publisher와 resource lifecycle publisher는 모두 여러 bean을 fan-out 한다.

```text
custom publisher
+ governance bridge publisher
-> 둘 다 실행
```

따라서 3계층은 자체 감사 저장소와 governance audit bridge를 동시에 붙일 수 있다.

## Recommended Combinations

### auth-server

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-stack-bom:1.0.6")
    implementation "io.github.jho951.platform:platform-governance-spring-boot-starter"
    implementation "io.github.jho951.platform:platform-security-issuer-starter"
    implementation "io.github.jho951.platform:platform-security-governance-bridge"
}
```

### authz-server

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-stack-bom:1.0.6")
    implementation "io.github.jho951.platform:platform-governance-spring-boot-starter"
    implementation "io.github.jho951.platform:platform-security-resource-server-starter"
    implementation "io.github.jho951.platform:platform-security-governance-bridge"
    implementation "io.github.jho951.platform:platform-security-policyconfig-bridge"
}
```

### user-server / block-server

```gradle
dependencies {
    implementation platform("io.github.jho951.platform:platform-stack-bom:1.0.6")
    implementation "io.github.jho951.platform:platform-governance-spring-boot-starter"
    implementation "io.github.jho951.platform:platform-security-resource-server-starter"
    implementation "io.github.jho951.platform:platform-resource-starter"
    implementation "io.github.jho951.platform:platform-resource-jdbc"
    implementation "io.github.jho951.platform:platform-resource-filestorage-adapter"
    implementation "io.github.jho951.platform:platform-resource-governance-bridge"
    implementation "io.github.jho951.platform:platform-security-governance-bridge"
}
```

## SecurityContext To ResourcePrincipal

3계층은 security principal을 도메인 resource principal로 변환한 뒤 resource runtime에 넘긴다.

```java
ResourcePrincipal toResourcePrincipal(SecurityContext securityContext) {
    ResourceOwner owner = new ResourceOwner("USER", securityContext.principal());
    Set<String> roles = securityContext.roles();
    Set<String> workspaceIds = Set.copyOf((Collection<String>) securityContext.attributes()
            .getOrDefault("workspaceIds", Set.of()));
    return new ResourcePrincipal(owner, roles, workspaceIds);
}
```

`ResourceOwner.type`, workspace 의미, 도메인 권한 사실은 3계층이 결정한다.
