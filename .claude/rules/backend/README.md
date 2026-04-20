# rules/backend

**Kotlin / Spring Boot 백엔드 전용** 규칙. frontend 에이전트(`frontend-builder` 등)에는 import 하지 않는다.

## 적용 대상

- agentFactory 자체 백엔드 (루트 `backend/`).
- `backend-builder` 가 생성하는 MVP 백엔드 (`generated/<jobId>/backend/`).

## 파일 구성

- [stack.md](stack.md) — 언어 · 런타임 · 프레임워크 버전 핀.
- [layering.md](layering.md) — 멀티모듈 레이아웃(domain / infrastructure / application) 과 의존 방향.
- [naming.md](naming.md) — 클래스 접미사 · 철자 · DTO 역할 구분.
- [domain-purity.md](domain-purity.md) — `domain` 모듈 프레임워크 의존 금지.
- [persistence.md](persistence.md) — R2DBC-first · Flyway JDBC 병존 · 트랜잭션 매니저 이름 필수.
- [error-handling.md](error-handling.md) — `IExceptionHandler<T>` 중앙 처리 패턴.

## 참조 방법

**전역 import 아님** — CLAUDE.md 에서 import 하지 않는다. 각 에이전트 frontmatter `rules:` 배열에 필요한 항목을 상대 경로로 명시:

```yaml
runtime:
  rules: [general, mvp-generation, backend/stack, backend/layering, backend/naming, backend/domain-purity, backend/persistence, backend/error-handling]
```

또는 폴더 전체를 한꺼번에:

```yaml
runtime:
  rules: [general, mvp-generation, "backend/*"]
```
