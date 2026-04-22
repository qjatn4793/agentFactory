---
name: modify-entity
description: 이미 생성된 MVP (`generated/<jobId>/`) 의 **기존 엔티티 필드를 확장**한다. 필드 추가 · nullable 완화 · 문자열 길이 확장 · enum 값 추가만 지원하며 ALTER TABLE 마이그레이션으로 처리. 이름변경 · 삭제 · 타입변경은 지원하지 않고 거부. backend/frontend 의 영향받는 파일만 Edit 으로 수정한 뒤 qa 재검증.
---

# /modify-entity

기존 MVP 의 엔티티에 **필드를 더한다**. `/add-entity` 가 "엔티티 통째 추가"라면, 이 스킬은 "엔티티 내부 형상 확장"을 담당한다.

## 인자

`/modify-entity <jobId> <Entity>: <변경 명세 (한 줄 또는 여러 줄)>`

예시:
- `/modify-entity 20260420-111639-31de Influencer: phone(string, nullable) 추가, birthDate(date, nullable) 추가`
- `/modify-entity 20260420-111639-31de SocialMediaAccount: postCount(integer, nullable) 추가, avgLikes(integer, nullable), avgComments(integer, nullable), engagementRate(decimal, nullable) 추가`
- `/modify-entity 20260420-111639-31de ContactLog: status enum 에 'blacklisted','confirmed','in_progress','none' 값 추가`
- `/modify-entity 20260420-111639-31de Tag: name 길이 100 → 255 확장`

`jobId` 생략 시 `generated/` 내 **가장 최근 디렉터리**를 사용하고 사용자에게 어떤 jobId 를 선택했는지 고지한다.

## 지원 연산 (허용 목록)

| 연산 | 설명 | 무중단 조건 |
|---|---|---|
| `ADD_FIELD` | 새 필드 추가 | **nullable=true** 또는 `DEFAULT` 값 명시. NOT NULL without default 거부 |
| `RELAX_NULLABILITY` | NOT NULL → NULLABLE | 항상 안전 |
| `EXTEND_STRING_LENGTH` | VARCHAR(n) → VARCHAR(m), m > n | 항상 안전 |
| `EXTEND_ENUM` | CHECK enum 값 추가 | 항상 안전 |

## 지원 안 하는 연산 (즉시 거부)

하나라도 포함되면 **전체 중단** + 이유 보고. 부분 적용하지 않는다.

- `DROP_FIELD` — 데이터 손실 위험. 수동 처리 필요.
- `RENAME_FIELD` — 스키마/모델/DTO/프론트 전영역 리네임은 범위 밖. 수동 처리 필요.
- `TIGHTEN_CONSTRAINT` — NULLABLE → NOT NULL, 길이 축소, enum 값 제거, UNIQUE 신규 부여. 기존 데이터 충돌 가능.
- `CHANGE_TYPE` — 타입 자체 변경 (string → int 등). 값 캐스팅 규칙 불분명.
- `ADD_NOT_NULL_WITHOUT_DEFAULT` — `ADD_FIELD` 인데 default 없이 NOT NULL 요구. 기존 row 가 있으면 ALTER 실패.

위가 필요하면 사용자에게 수동 마이그레이션 작성을 권장하고 스킬은 종료한다.

## 수행 절차

### 1. 입력 검증

- `generated/<jobId>/artifacts/spec.json` 로드 (없으면 중단).
- 지정된 `<Entity>` 가 `spec.entities[]` 에 있는지 확인 (없으면 중단).
- 사용자 명세를 연산 목록으로 파싱 (자연어 → `[{type, field, ...}]`). Claude 가 이 해석도 담당.
- **허용 목록 전수 검증**. 거부 항목이 있으면 enumerate 해서 사용자에게 반환 후 종료.
- 기존 `backend/.../resources/db/migration/` 의 최대 V 번호를 스캔해 다음 번호 `V<N+1>` 을 고정.

### 2. mvp-planner 재호출 (**변경 모드**)

프롬프트에 기존 `spec.json` 전체 + 대상 엔티티명 + 연산 목록 + 지시문:

> 지정된 엔티티의 `fields` 배열(또는 기존 field 의 제약)만 수정한다. 다른 entities · relations · screens · apis 는 절대 수정하지 않는다. 반환 JSON 은 변경 후 **해당 엔티티 1개의 전체 객체** + 연산 로그:
> ```json
> {
>   "entity": { "name": "...", "description": "...", "fields": [...] },
>   "operations": [
>     { "type": "ADD_FIELD", "field": "phone", "type_": "string", "nullable": true, "default": null },
>     { "type": "EXTEND_ENUM", "field": "status", "added": ["blacklisted","confirmed"] }
>   ]
> }
> ```

반환된 entity 로 `spec.json` 의 해당 엔티티 덮어쓰기. 다른 엔티티는 건드리지 않는다. 백업은 `artifacts/spec.<timestamp>.json` 으로 복사.

### 3. schema-designer 재호출 (**증분 ALTER 모드**)

프롬프트: 업데이트된 `spec.json` + `schema.json` (기존) + operations + 다음 마이그레이션 번호 `V<N+1>` + 지시문:

> 연산 목록을 **단일 새 마이그레이션 파일** `V<N+1>__<entity>_<summary>.sql` 로 emit 한다. 기존 V00* 은 읽기 전용. 오직 `ALTER TABLE` 만 사용. `upSql` 과 `downSql` 둘 다 emit.
>
> 필수 규칙:
> - `ADD_FIELD`: 반드시 `NULL` 또는 `NOT NULL DEFAULT <val>` 중 하나. 인덱스가 필요한 필드면 별도 `CREATE INDEX` 를 같은 파일에 포함.
> - `EXTEND_ENUM`: MySQL 의 CHECK 제약은 `DROP CONSTRAINT` → `ADD CONSTRAINT` (값 확장만. 기존 값 모두 포함 필수).
> - `EXTEND_STRING_LENGTH`: `ALTER TABLE ... MODIFY COLUMN ... VARCHAR(new)`
> - `RELAX_NULLABILITY`: `ALTER TABLE ... MODIFY COLUMN ... NULL`
>
> 조건을 만족 못 시키면 중단 보고 (file 을 생성하지 않음).

반환 SQL 을 `backend/application/api/src/main/resources/db/migration/V<N+1>__*.sql` 로 기록. `schema.json` 의 `migrations[]` 에 append, `tables[].columns[]` 에도 변경 반영.

### 4. backend-builder 재호출 (**부분 수정 모드**)

프롬프트: 업데이트된 spec + schema delta + operations + workspaceDir + 지시문:

> 엔티티 `<Name>` 에 관련된 **기존 파일만 Edit 툴로 수정**한다. 새 파일 생성 금지, 전체 덮어쓰기 금지. 마이그레이션 SQL 은 이미 작성됨 — 재생성 금지.
>
> 수정 대상 (해당 파일이 존재할 때만):
> - `domain/.../model/<Name>.kt`
> - `domain/.../command/<Name>InsertCommand.kt`
> - `domain/.../command/<Name>UpdateCommand.kt`
> - `domain/.../query/<Name>*Query.kt` (새 필드가 검색/정렬 대상인 경우에만)
> - `domain/.../enums/<EnumName>.kt` (EXTEND_ENUM 시)
> - `infrastructure/.../persistence/entity/<Name>Entity.kt`
> - `infrastructure/.../persistence/adaptor/<name>/<Name>Adaptor.kt` (toDomain / toEntity 매핑 동기화)
> - `application/api/.../presentation/dto/<name>/<Name>CreateRequest.kt`
> - `application/api/.../presentation/dto/<name>/<Name>UpdateRequest.kt`
> - `application/api/.../presentation/dto/<name>/<Name>Response.kt`
> - `application/api/.../presentation/dto/<name>/<Name>DetailResponse.kt` (있으면)
> - `application/api/.../presentation/controller/<Name>Controller.kt` (Command 생성부에 필드 전달 추가)
>
> 연산별 구체 룰:
> - `ADD_FIELD`: 모든 레이어에 필드 추가. Kotlin 기본값은 `= null` (nullable). Request 에 `jakarta.validation` 제약(`@Size`, `@Email` 등) 필요 시만 추가. Response 에 노출. Adaptor 의 매핑 extension 갱신.
> - `EXTEND_ENUM`: 해당 enum class 에 값 추가. `from(raw: String?)` 매핑 확장. 해당 enum 을 쓰는 Query 도 확장값 허용.
> - `RELAX_NULLABILITY`: 타입을 nullable(`T?`) 로 변경. 호출부의 non-null 단언 제거.
> - `EXTEND_STRING_LENGTH`: Request 의 `@Size(max=...)` 값 갱신. 도메인/엔티티 코드는 변경 없음.
>
> rules/backend/domain-purity.md · rules/backend/persistence.md · rules/backend/naming.md 를 위반하지 않도록 주의 (Adaptor 철자, Persistable, OffsetDateTime UTC 등).

### 5. frontend-builder 재호출 (**부분 수정 모드**)

프롬프트: 업데이트된 spec + operations + workspaceDir + 지시문:

> 엔티티 `<Name>` 에 관련된 **기존 파일만 Edit 툴로 수정**한다. 새 파일 생성 금지, 사이드바/router 수정 금지.
>
> 수정 대상:
> - `src/schemas/<name>.ts` — zod 스키마 `<Name>CreateSchema`/`<Name>UpdateSchema` 에 필드 추가
> - `src/api/<name>.ts` — 요청 바디 타입에 필드 추가
> - `src/api/types.ts` — 응답 타입 `<Name>` 에 필드 추가
> - `src/routes/<name>/<Name>FormView.tsx` — 입력 필드 추가 (FormField 컴포넌트 패턴)
> - `src/routes/<name>/detail.tsx` — 신규 필드 표시 영역 추가
> - `src/routes/<name>/list.tsx` — 리스트 표시 대상인 경우에만 컬럼 추가
> - `src/lib/enums.ts` — EXTEND_ENUM 시 label/옵션 배열 갱신
>
> 연산별 구체 룰:
> - `ADD_FIELD`:
>   - string/text: `<Input>` / `<Textarea>`
>   - integer: `<Input inputMode="numeric">` + numeric filter
>   - decimal: `<Input inputMode="decimal">` + regex filter
>   - boolean: shadcn `<Checkbox>` / `<Switch>`
>   - date: `<Input type="date">`
>   - datetime: `<Input type="datetime-local">`
>   - enum: `<Select>` + `enums.ts` 옵션 배열 사용
> - `EXTEND_ENUM`: `src/lib/enums.ts` 의 해당 enum 배열과 label 매핑에 값 추가. 기존 Select 는 자동으로 반영됨.
> - `RELAX_NULLABILITY`: zod `.optional()` 또는 `.nullable()` 적용.
> - `EXTEND_STRING_LENGTH`: zod `.max(new)` 로 갱신.
>
> rules/frontend/stack.md (TanStack Router validateSearch, shadcn 패턴) 위반 금지.

### 6. qa-reviewer 호출

- 전체 아티팩트 재검증. `artifacts/review.json` 덮어쓰기 (백업 `review.<timestamp>.json`).
- `fail` 이어도 **파일을 자동 롤백하지 않는다** — 사용자가 판단. 보고에 명확히 표기.

## 사용자 보고 (한 블록)

```
modify-entity: <jobId> — <EntityName>
├─ operations: <N>건
│   ├─ ADD_FIELD x<k>: phone, birthDate, ...
│   ├─ EXTEND_ENUM x<k>: status +['blacklisted']
│   └─ RELAX_NULLABILITY x<k>: ...
├─ spec: entity "<EntityName>" fields <before>→<after>, 백업 spec.<ts>.json
├─ schema: V<N+1>__<entity>_<summary>.sql (upSql/downSql)
├─ backend: <K> files edited
├─ frontend: <K> files edited
└─ qa: <pass|fail> — <summary>
```

## 안전장치

- **파괴적 연산 자동 거부**: DROP / RENAME / TIGHTEN / TYPE_CHANGE / NOT_NULL_WITHOUT_DEFAULT 포함 시 전체 중단.
- **마이그레이션 번호 충돌**: 이미 같은 번호 파일이 있으면 즉시 중단 + 사용자에게 원인 확인 요청. 자동으로 번호를 건너뛰지 않는다.
- **spec.json 백업 필수**: 수정 전 `spec.<timestamp>.json` 으로 복사.
- **V001 포함 기존 마이그레이션 편집 금지**: 오직 새 V 번호 추가.
- **qa fail 시 파일 유지**: 수동 판단에 맡김. 단 보고에 "fail — 수동 검토 필요" 명시.
- **Adaptor 철자는 `Adaptor`**: rules/backend/naming.md 준수 (Adapter 금지).

## 하지 말 것

- 신규 엔티티 추가 — `/add-entity` 사용.
- relations · screens · apis 변경 — `/add-entity` 또는 `/polish` 사용.
- V001 을 포함한 기존 마이그레이션 편집.
- enum 값 **제거** — 기존 데이터가 그 값을 가질 수 있음.
- 전체 파이프라인 재실행. 이 스킬은 항상 부분 수정.
- spec.json 에 없는 엔티티에 대한 변경 시도 — 에러 후 종료.
- 여러 엔티티를 한 번에 변경 — 한 호출에 엔티티 하나만. 여러 엔티티가 필요하면 호출을 나눈다.
