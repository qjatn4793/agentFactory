---
name: generate-mvp
description: 한 줄 아이디어를 받아 전체 MVP 생성 파이프라인(planner → schema → backend → frontend → qa)을 실행하고 `generated/<jobId>/` 에 산출물 + 단계별 JSON 아티팩트를 남긴다.
---

# /generate-mvp

## 인자

사용자 입력 전체를 **아이디어 한 줄**로 간주한다. 입력이 없으면 중단하고 아이디어를 요청한다.

예: `/generate-mvp 1인 카페 사장이 원두 재고와 주간 매출을 관리하는 도구`

## 수행 절차

### 0. 작업 공간 생성

- `jobId` = `yyyymmdd-HHMMSS-<4자 랜덤hex>` (로컬 시각 기준).
- `workspaceDir` = `generated/<jobId>/`.
- `workspaceDir/artifacts/` 생성 — 단계별 JSON 저장.
- `workspaceDir/backend/`, `workspaceDir/frontend/` 는 각 빌더가 직접 생성.

### 1. mvp-planner 호출

- `Agent(subagent_type: "mvp-planner", prompt: "<아이디어 원문>")`.
- 반환 본문에서 JSON 블록 추출 → 스키마 검증 (top-level 키: `title, summary, entities, relations, screens, apis, auth`).
- `artifacts/spec.json` 에 저장.
- 실패 시 중단하고 이유를 보고.

### 2. schema-designer 호출

- 프롬프트에 `spec.json` 전체를 포함.
- 반환 JSON 검증 (`tables`, `migration.upSql`, `migration.downSql`).
- `artifacts/schema.json` 에 저장.

### 3. backend-builder 호출

- 프롬프트에 `spec.json`, `schema.json`, `workspaceDir` 절대경로를 넘긴다.
- 에이전트가 `workspaceDir/backend/` 아래에 파일을 직접 쓴다.
- 반환 요약(JSON)을 `artifacts/backend.json` 에 저장.
- 검증: `backend/src/main/resources/db/migration/V001__init.sql` 내용이 `schema.json.migration.upSql` 과 바이트 단위로 동일해야 한다.

### 4. frontend-builder 호출

- 프롬프트에 `spec.json`, `artifacts/backend.json`, `workspaceDir` 를 넘긴다.
- 파일은 `workspaceDir/frontend/` 에.
- 반환을 `artifacts/frontend.json` 에 저장.

### 5. qa-reviewer 호출

- 모든 아티팩트 경로와 `workspaceDir` 를 넘긴다.
- 반환 `ReviewReport` 를 `artifacts/review.json` 에 저장.
- `status == "fail"` 이면 issue 목록을 사용자에게 보고하되 **자동 재시도는 하지 않는다** (사용자가 어떤 이슈를 수정할지 결정).
- fail 이면 deployer 단계를 **건너뛴다** (엄격 모드).

### 6. deployer 호출 (qa pass 일 때만)

- 프롬프트에 `MvpSpec`, `BackendArtifacts`, `FrontendArtifacts`, `ReviewReport` (status=pass), `workspaceDir`, `jobId` 를 넘긴다.
- deployer 가 `backend/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`, 루트 `docker-compose.yml`, `.env.docker` 를 작성한다.
- 반환 `DeployArtifacts` 를 `artifacts/deploy.json` 에 저장.
- 비밀값(`ADMIN_PASSWORD`, `DB_PASSWORD`) 은 `.env.docker` 에만 있고 터미널에 출력하지 않는다.

## 사용자 보고 형식

완료 시 아래를 한 번에 출력:

```
MVP 생성 완료 — <jobId>
├─ spec:     entities=N, screens=M, apis=K
├─ schema:   tables=N, migration=V001
├─ backend:  files=K
├─ frontend: files=K
├─ qa:       <pass|fail>, issues=N
└─ deploy:   docker-compose.yml + Dockerfiles 작성됨 (qa pass 전제)

다음 단계: /run-mvp <jobId>   (docker compose up → URL 출력)
```

qa fail 이면 deploy 행을 "건너뜀 (qa fail)" 로 표시하고 `/run-mvp` 안내도 생략.

## 실패 처리

- **어느 단계든 JSON 파싱 실패** → 에이전트를 **한 번만** 재호출 (프롬프트에 "이전 응답이 JSON 스키마를 만족하지 않았다. 재시도."). 두 번째도 실패하면 중단.
- **빌더 도중 파일 쓰기 실패** → 중단, `workspaceDir` 삭제하지 않음 (디버깅용 보존).
- **qa fail** → 산출물은 유지. 사용자가 `/review-generated <jobId>` 또는 수작업 수정 후 `/generate-mvp` 재실행.

## 하지 말 것

- `generated/<jobId>/` 밖에 파일 쓰기.
- 이전 jobId 덮어쓰기 — 매 실행마다 새 jobId.
- qa 실패를 자동으로 에이전트 재호출로 "고치려" 하기 — 루프 위험.
- 에이전트 프롬프트에 rules 본문을 매번 수동 inline — 런타임 로더가 할 일. 이 스킬에서는 Claude Code 서브에이전트 호출이므로 rules는 이미 CLAUDE.md 를 통해 메인 컨텍스트에 있음.
- qa fail 상태에서 deployer 호출 (엄격 정책 — 빌드 검증 실패면 배포 안 함).
- `docker compose up` 을 이 스킬 안에서 자동 실행. `/generate-mvp` 는 파일 생성까지, 실제 기동은 별도 `/run-mvp` 로 분리 (긴 빌드 시간 + 사용자가 배포 타이밍을 제어).
