---
name: add-entity
description: 이미 생성된 MVP (`generated/<jobId>/`) 에 엔티티 하나를 추가한다. spec.json 을 업데이트하고 schema/backend/frontend 의 **영향받는 부분만** 재생성한 뒤 qa-reviewer로 검증.
---

# /add-entity

## 인자

`/add-entity <jobId> <엔티티 설명 한 줄>`

예: `/add-entity 20260417-143022-a3f1 공급자(Supplier): 이름, 연락처, 메모, Product와 oneToMany`

`jobId` 생략 시 `generated/` 최신 디렉터리 사용 (고지).

## 수행 절차

### 1. 기존 스펙 로드

- `generated/<jobId>/artifacts/spec.json` 로드. 없으면 중단.
- 엔티티 이름 충돌 체크 (같은 name 이 있으면 중단; `/add-entity` 는 추가 전용).

### 2. mvp-planner 재호출 (**추가 모드**)

- 프롬프트에 기존 `spec.json` 전체 + 사용자 입력 엔티티 설명 + 지시문:
  > 기존 MvpSpec에 엔티티 1개를 추가한다. **기존 entities/relations/screens/apis 는 절대 수정하지 않는다.** 추가된 엔티티에 대한 entities[+1], relations[+N], screens[+1~4], apis[+N] 만 새로 만든다. 반환 JSON 은 추가되는 delta 만 포함한다:
  > ```json
  > { "entities": [...추가분], "relations": [...추가분], "screens": [...추가분], "apis": [...추가분] }
  > ```
- 반환 delta 를 기존 spec 에 머지 → `artifacts/spec.json` 덮어쓰기 (백업: `spec.<timestamp>.json`).

### 3. schema-designer 재호출 (**증분 마이그레이션 모드**)

- 프롬프트: 업데이트된 `spec.json` 전체 + `artifacts/schema.json` (기존) + 지시문:
  > 기존 테이블/FK는 건드리지 않는다. 새 엔티티에 대한 테이블 + 필요한 joinTable + 인덱스만 생성한다. 마이그레이션은 **새 버전** (V002, V003, ... — 기존 최대 버전 + 1) 으로 emit. `upSql`/`downSql` 도 신규 객체만.
- 반환 delta 를 `schema.json` 에 머지 (tables 추가, migrations 배열에 append).
- `backend/src/main/resources/db/migration/V00N__add_<entity>.sql` 파일 생성, `upSql` 그대로 기록.

### 4. backend-builder 재호출 (**부분 생성 모드**)

- 프롬프트: spec + schema + workspaceDir + 지시문:
  > 신규 엔티티 `<Name>` 에 대한 Entity/Repository/Service/Controller/DTO 파일만 생성. 기존 파일은 읽지도 말고 쓰지도 말 것. 마이그레이션 SQL 파일은 이미 있으므로 재생성 금지.

### 5. frontend-builder 재호출 (**부분 생성 모드**)

- 프롬프트: spec + backend summary + workspaceDir + 지시문:
  > 신규 엔티티 `<Name>` 에 대한 `routes/<name>/*`, `api/<name>.ts`, `schemas/<name>.ts` 만 생성. 사이드바 네비게이션 항목 추가는 `App.tsx` 또는 `router.tsx` 에서 한 줄 추가 Edit 로 처리.

### 6. qa-reviewer 호출

- 전체 아티팩트 재검증. 결과 `artifacts/review.json` 덮어쓰기.

## 사용자 보고

```
add-entity: <jobId> — <EntityName> 추가
├─ spec: +1 entity, +N relations, +M screens, +K apis
├─ schema: +N tables, migration V00N__add_<entity>
├─ backend: +K files
├─ frontend: +K files (sidebar 항목 추가됨)
└─ qa: <pass|fail>
```

## 하지 말 것

- 기존 테이블에 컬럼 추가 (이 스킬은 **신규 엔티티 전용**). 기존 엔티티 필드 확장은 `/modify-entity` 사용.
- 이름 충돌 시 자동 리네임. 반드시 사용자에게 중단 보고.
- 전체 파이프라인 재실행. 기존 파일 덮어쓰기 금지 (새 파일만 추가).
- 마이그레이션 V001 수정. 항상 새 버전 번호.
