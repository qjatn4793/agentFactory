# agentFactory

한 줄 아이디어를 입력하면 관리자형(admin) 웹앱 MVP를 자동 생성하는 멀티 에이전트 서비스.

> 예: `"회원이 책을 빌려가는 도서 대여 관리 시스템"` → 백엔드(Kotlin/Spring Boot) + 프론트엔드(TypeScript/React) + PostgreSQL + Docker Compose 한 세트가 `generated/<jobId>/` 에 떨어지고, 명령 하나로 뜬다.

## 무엇을 하는가

1. 사용자가 한 줄 아이디어를 던진다.
2. Claude 기반 서브에이전트들이 **단계별 계약**을 따라 릴레이한다:
   - `mvp-planner` → `schema-designer` → `backend-builder` → `frontend-builder` → `qa-reviewer` → `deployer`
3. 각 단계는 자유 텍스트가 아닌 **구조화된 JSON 아티팩트**만 소비해 다음 단계로 넘긴다.
4. QA가 통과한 MVP만 Docker Compose 번들로 포장되어, 로컬에서 `docker compose up` 한 번으로 실행 가능한 상태가 된다.

관리자형 MVP의 기본 형상(엔티티당 리스트/상세/폼/삭제, 검색/필터/페이지네이션, 단일 admin 인증 스텁)을 전 파이프라인이 공통 가정으로 공유한다.

## 기술 스택

| 레이어 | 스택 | 비고 |
|---|---|---|
| Frontend | TypeScript + React + Vite | 사용자 대시보드 (아이디어 입력 / 생성 진행률 / 결과 확인) |
| Backend | Kotlin + Spring Boot (Gradle Kotlin DSL) | 아이디어 수신 → 에이전트 오케스트레이션 → 산출물 관리 |
| 생성 엔진 | Claude (Claude API / Agent SDK) | 백엔드에서만 호출 (키 노출·중앙 관측 이유) |
| 산출물 DB | PostgreSQL + Flyway | 생성된 MVP 의 기본 DB |
| 패키징 | Docker Compose + nginx | 생성 결과를 단일 번들로 기동 |

> 스택은 고정이다 (`.claude/rules/stack.md`). TS → JS, Kotlin → Java 회귀 금지. 프론트엔드에서 `@anthropic-ai/sdk` 직접 사용 금지 (훅으로 차단).

## 파이프라인

```
idea
  └─► mvp-planner       → MvpSpec          (엔티티 · 관계 · 화면 · API · 인증)
        └─► schema-designer → SchemaPlan    (PostgreSQL DDL + Flyway 마이그레이션)
              └─► backend-builder → BackendArtifacts  (Kotlin + Spring Boot + OpenAPI)
                    └─► frontend-builder → FrontendArtifacts  (TS admin UI)
                          └─► qa-reviewer → ReviewReport      (스펙↔스키마↔API↔UI 정합성)
                                └─► deployer → DeployArtifacts  (qa pass 일 때만)
```

단계 분리가 디버깅/재시도 가능성의 핵심 — 한 단계가 실패해도 해당 단계의 JSON만 갱신해 재개할 수 있다.

## 디렉터리

```
agentFactory/
  CLAUDE.md                # 프로젝트 지침 (Claude Code가 자동 로드)
  .claude/
    agents/                # 서브에이전트 정의 (mvp-planner, schema-designer, ...)
    skills/                # 사용자 호출형 스킬 (/generate-mvp, /run-mvp, ...)
    rules/                 # 에이전트/스킬 공통 규칙 (general, stack, mvp-generation)
    hooks/                 # 스택 강제 · 포맷 · 에이전트 호출 로깅
    settings.json          # 훅 배선
  frontend/                # (계획) TypeScript 대시보드
  backend/                 # (계획) Kotlin 오케스트레이터
  generated/<jobId>/       # 생성된 MVP 산출물 (런타임)
    artifacts/             # 단계별 JSON (spec / schema / backend / frontend / review / deploy)
    backend/               # Spring Boot 프로젝트 + Dockerfile
    frontend/              # TS/React 프로젝트 + Dockerfile + nginx.conf
    docker-compose.yml
    .env.docker            # 비밀값 (gitignore 대상)
```

현재는 `.claude/` 레이어로 **Claude Code 위에서 동작**하는 형태. `frontend/` · `backend/` 본체는 이후 구현 단계에서 채워진다.

## 사용법 (Claude Code 기반)

Claude Code 세션에서 스킬로 전체 흐름을 제어한다.

### MVP 생성

```
/generate-mvp 회원이 책을 빌려가는 도서 대여 관리 시스템
```

→ `generated/<jobId>/` 에 아티팩트 + 백엔드/프론트엔드/compose 번들 생성.

### 실행 / 조회 / 중지

| 스킬 | 역할 |
|---|---|
| `/run-mvp <jobId>` | `docker compose up --build -d` + 호스트 포트 탐지 + 접속 URL 출력 |
| `/list-mvps` | 실행 중 MVP 목록과 URL 테이블 |
| `/stop-mvp <jobId>` | 컨테이너만 내림 (postgres 볼륨은 유지 — 데이터 보존) |
| `/reset-mvp <jobId>` | 볼륨까지 완전 삭제 (파괴적 · 확인 필수) |
| `/review-generated <jobId>` | QA 재실행 (수작업 수정 후 검증 용도) |
| `/add-entity <jobId> ...` | 기존 MVP 에 엔티티 1개 증분 추가 (기존 파일 비파괴) |

## 설계 원칙

- **단계 계약**: 다음 에이전트는 이전 에이전트의 JSON만 신뢰. 자유 텍스트 재파싱 금지.
- **격리된 작업공간**: 생성물은 프로젝트 트리가 아닌 `generated/<jobId>/` 로만 쓴다.
- **관측 가능성**: 모든 작업은 `jobId` 로 추적. 서브에이전트 호출은 `.claude/logs/agent-calls.log` 에 JSONL 로 적재.
- **스택 강제**: Write/Edit 전에 훅이 `.jsx`, `.java`, 프론트엔드의 Claude SDK import 를 차단.
- **범위 준수**: "TODO 나중에 채움" 같은 반쪽 스텁을 남기지 않는다. 범위에 없으면 생성하지 않는다.

세부 규칙은 `.claude/rules/` 참고:
- [general.md](.claude/rules/general.md) — 작업 태도·커뮤니케이션·파괴적 작업
- [stack.md](.claude/rules/stack.md) — TS/Kotlin 고정, Claude 호출 경로
- [mvp-generation.md](.claude/rules/mvp-generation.md) — 파이프라인·기본 형상·금기

## 상태

초기 단계. 현재 동작하는 계층은 `.claude/` 기반 파이프라인(Claude Code 위) 과 생성된 MVP 의 Docker Compose 실행까지. `frontend/` 대시보드와 `backend/` 오케스트레이터(Kotlin) 는 다음 마일스톤.
