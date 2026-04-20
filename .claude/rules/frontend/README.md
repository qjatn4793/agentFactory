# rules/frontend

**TypeScript 프론트엔드 전용** 규칙. backend 에이전트(`backend-builder` 등)에는 import 하지 않는다.

## 적용 대상

- `frontend-builder` 가 생성하는 MVP 프론트엔드 (`generated/<jobId>/frontend/`).
- 향후 agentFactory 자체 대시보드 (루트 `frontend/`) 가 생기면 동일 적용.

## 파일 구성

- [stack.md](stack.md) — TypeScript · 프레임워크 · Claude 호출 경로.

## 참조 방법

**전역 import 아님** — CLAUDE.md 에서 import 하지 않는다. 각 에이전트 frontmatter `rules:` 배열에서 명시:

```yaml
runtime:
  rules: [general, mvp-generation, frontend/stack]
```
