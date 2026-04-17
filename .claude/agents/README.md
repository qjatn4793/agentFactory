# agents/

프로젝트 전용 서브에이전트 정의. Claude Code가 이 디렉터리의 `*.md` 파일을 자동으로 로드한다.

## 파일 형식

각 에이전트는 하나의 `.md` 파일 + YAML frontmatter로 정의한다.

```markdown
---
name: mvp-planner
description: 한 줄 아이디어를 받아 MVP 범위 · 화면 목록 · 데이터 모델 초안을 산출한다. 신규 아이디어를 받을 때 가장 먼저 호출.
tools: Read, Grep, Glob, WebFetch
model: sonnet
---

<시스템 프롬프트 본문>
- 역할과 목표
- 입력 / 출력 계약
- 지켜야 할 규칙 (rules/ 에서 필요한 것 인용)
```

## 이중 용도 (Claude Code + 런타임)

이 디렉터리의 `*.md` 는 **두 곳에서 소비**된다:

1. **Claude Code**: frontmatter의 `name`, `description`, `tools`, `model` 을 읽어 서브에이전트로 자동 등록. 개발 중 위임에 사용.
2. **agentFactory 런타임(Kotlin 백엔드)**: frontmatter의 `runtime:` 섹션 + 본문을 읽어 실제 MVP 생성 파이프라인에서 호출.

## runtime frontmatter 규약

```yaml
runtime:
  model: claude-sonnet-4-6 | claude-opus-4-7  # 런타임에서 사용할 정확한 모델 ID
  consumes: [InputArtifact, ...]              # 입력 아티팩트 타입
  produces: OutputArtifact                    # 출력 아티팩트 타입
  depends_on: [agentName, ...]                # 이 에이전트보다 먼저 실행되어야 하는 것
  rules: [general, stack, mvp-generation]     # 런타임이 prepend할 rules/<name>.md
```

## 파이프라인

```
idea
  └─► mvp-planner       → MvpSpec
        └─► schema-designer  → SchemaPlan
              └─► backend-builder  → BackendArtifacts
                    └─► frontend-builder → FrontendArtifacts
                          └─► qa-reviewer → ReviewReport
                                └─► deployer → DeployArtifacts  (qa pass 일 때만)
```

각 단계는 이전 단계의 **구조화된 JSON 산출물**만 신뢰한다 (자유 텍스트 재파싱 금지).

## 현재 에이전트

- [mvp-planner.md](mvp-planner.md) — 한 줄 아이디어 → MvpSpec
- [schema-designer.md](schema-designer.md) — MvpSpec → SchemaPlan (PostgreSQL + Flyway)
- [backend-builder.md](backend-builder.md) — MvpSpec + SchemaPlan → Kotlin/Spring Boot 파일
- [frontend-builder.md](frontend-builder.md) — MvpSpec + BackendArtifacts → TS/React/Vite 파일
- [qa-reviewer.md](qa-reviewer.md) — 산출물 정합성 감사 → ReviewReport
- [deployer.md](deployer.md) — Dockerfile + nginx.conf + docker-compose.yml (qa pass 전제)
