# backend/naming

클래스 · 파일 · 컬럼 명명 규칙. 철자와 접미사는 **엄격하게 고정**.

## 철자 (자주 틀리는 것)

- **`Adaptor`** (~~`Adapter`~~ 금지). 인프라 포트 구현체 전부 이 철자.

## 클래스 접미사 (레이어별)

| 레이어 · 패키지 | 접미사 | 예 |
|---|---|---|
| domain/command | `*Command` | `TodoInsertCommand`, `TodoUpdateCommand` |
| domain/query | `*Query` | `TodoFindAllBySearchQuery`, `TodoCountBySearchQuery`, `TodoFindAllBySearchPagedQuery` |
| domain/model | (없음) | `Todo`, `Category` |
| domain/service | `*Service` | `TodoService` |
| domain/persistence | `I*Port` (접두 I + Port) | `ITodoPort`, `ICategoryPort` |
| domain/external | `I*Port` | `IClaudePort` |
| domain/exception | `*Exception` + `*ErrorCode` | `AgentFactoryException`, `TodoErrorCode` |
| domain/aggregate (옵션) | `*AggregateService`, `*ViewAggregate` | `TodoAggregateService` |
| infrastructure/entity | `*Entity` | `TodoEntity`, `CategoryEntity` |
| infrastructure/repository/r2dbc | `*Repository` | `TodoRepository` |
| infrastructure/repository/jooq | `*JooqRepository` | `TodoJooqRepository` |
| infrastructure/adaptor | `*Adaptor` | `TodoAdaptor`, `CategoryAdaptor` |
| application/controller | `*Controller` | `TodoController` |
| application/dto | `*Request`, `*Response` | `TodoCreateRequest`, `TodoResponse` |

## 파일 · 패키지

- 파일명 = 대표 클래스명 (`TodoService.kt` 에 `class TodoService`).
- 패키지명: 스네이크케이스 대신 소문자 compact (`todotag` 보다 `todo_tag` 대신 `todotag` 또는 `tag`), 엔티티별 하위 패키지로 분리 (`domain.service.todo`, `domain.service.category`).

## DTO 역할 구분 (혼용 금지)

| 용도 | 타입 | 방향 |
|---|---|---|
| API 요청 바디 | `*Request` | client → controller |
| API 응답 바디 | `*Response` | controller → client |
| 도메인 입력 | `*Command` | controller/aggregate → service |
| 도메인 조회 | `*Query` | controller/aggregate → service |

Controller 는 `*Request` → `*Command` 로 매핑 후 service 호출. Service 는 도메인 모델 반환, controller 에서 `*Response` 로 매핑.

## DB 컬럼 명명 (MVP 기본)

- 스네이크케이스 복수 테이블명 (`users`, `todos`, `todo_tags`).
- 컬럼: 스네이크케이스 (`created_at`, `due_date`, `category_id`).
- FK: `<참조테이블단수>_id` (예: `category_id`).

## 트랜잭션 매니저 이름

- `@Transactional(transactionManager = "<name>R2dbcTransactionManager")` — 단일 DB 여도 **이름 명시 필수**.
- MVP 기본: `"r2dbcTransactionManager"`.

## 하지 말 것

- `Adapter` 철자 사용 — 프로젝트 전체에서 `Adaptor` 로 통일.
- `*Service` 를 infrastructure 에서 사용 — service 는 domain 전용.
- `*Dto` 같은 포괄 접미사 — 역할별 `*Request` / `*Response` / `*Command` / `*Query` 로 분리.
- 한 클래스에 `*Request` 와 `*Response` 를 같이 선언 — 파일 분리.
