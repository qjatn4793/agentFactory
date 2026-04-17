---
name: review-generated
description: 이미 생성된 MVP (`generated/<jobId>/`) 에 대해 qa-reviewer 를 다시 실행해 정합성 리포트를 낸다. 수작업 수정 후 검증 용도.
---

# /review-generated

## 인자

`/review-generated <jobId>`

- `jobId` 가 없으면 `generated/` 하위 디렉터리 중 **가장 최근** 것을 자동 선택하고, 그 사실을 사용자에게 한 줄로 고지.
- `generated/<jobId>/` 가 없으면 중단.

## 수행 절차

### 1. 아티팩트 로드

필수 파일 존재 확인:

- `artifacts/spec.json`
- `artifacts/schema.json`
- `artifacts/backend.json`
- `artifacts/frontend.json`

하나라도 없으면 "이 MVP 는 파이프라인을 끝까지 실행하지 않았다" 고 보고하고 중단.

### 2. qa-reviewer 호출

- `Agent(subagent_type: "qa-reviewer", prompt: <4개 아티팩트 JSON + workspaceDir 절대경로>)`.
- 반환 `ReviewReport` 를 `artifacts/review.json` **덮어쓰기** (이전 리뷰는 `artifacts/review.<timestamp>.json` 으로 백업).

### 3. 보고

`ReviewReport.summary` + issue 목록을 사용자에게 출력. 형식:

```
review: <jobId> — <pass|fail>
coverage: entities N/N, apis M/M, screens K/K

[error] backend: UserController에 DELETE /api/users/:id 누락
  fix: UserController.kt에 `@DeleteMapping("/{id}")` 핸들러 추가

[warning] frontend: schemas/user.ts 의 email 이 optional 이지만 스펙은 nullable=false
  fix: zod 스키마를 z.string().email() 로 (optional 제거)
```

error 가 0 이면 상단에 `✓ pass` 한 줄만.

## 하지 말 것

- 이슈를 자동 수정. 이 스킬은 **보고만** 한다. 수정은 사용자가 명시적으로 요청하거나 `/generate-mvp` 재실행으로.
- qa-reviewer 외 다른 에이전트 호출.
- `artifacts/` 외부 파일 변경.
