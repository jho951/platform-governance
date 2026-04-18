# Docs

## 핵심 기준

- 2계층은 audit, policy config, plugin policy engine, violation handling을 조립한다.
- 3계층은 서비스 정책 key, 정책 값, domain plugin, audit sink를 제공한다.
- 3계층 기본 진입점은 `platform-governance-bom` + `platform-governance-starter`다.
- `service-role-preset`은 서비스 이름이 아니라 governance 주 역할을 표현한다.
- 운영에서는 audit sink, audit context, service identity, policy config, fatal handler 정책을 fail-fast로 검증한다.

## 읽는 순서

1. [architecture.md](architecture.md)
2. [modules.md](modules.md)
3. [governance-model.md](governance-model.md)
4. [configuration.md](configuration.md)
5. [dependency-basis.md](dependency-basis.md)
6. [extension-guide.md](extension-guide.md)
7. [private-publish.md](private-publish.md)
8. [troubleshooting.md](troubleshooting.md)
9. [examples/auth-server.md](examples/auth-server.md)
10. [quality-gates.md](quality-gates.md)
