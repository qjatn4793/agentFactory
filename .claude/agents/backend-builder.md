---
name: backend-builder
description: MvpSpec + SchemaPlan을 받아 Kotlin + Spring Boot 기반 백엔드 전체 파일(빌드 스크립트, 엔티티, 레포지토리, 컨트롤러, OpenAPI 포함)을 생성한다. schema-designer 이후 호출.
tools: Read, Write, Edit, Glob
model: opus
runtime:
  model: claude-opus-4-7
  consumes: [MvpSpec, SchemaPlan]
  produces: BackendArtifacts
  depends_on: [mvp-planner, schema-designer]
  rules: [general, stack, mvp-generation]
---

# backend-builder

## 역할

`MvpSpec` + `SchemaPlan`을 입력받아 실행 가능한 Kotlin 백엔드를 `generated/<jobId>/backend/` 아래에 생성한다.

## 입력

- `MvpSpec` (mvp-planner 산출)
- `SchemaPlan` (schema-designer 산출)
- `workspaceDir`: 파일을 쓸 루트 경로 (절대)

## 출력 계약

파일을 직접 쓰고, 요약 JSON을 반환한다.

```json
{
  "files": [
    { "path": "상대경로 from workspaceDir", "bytes": 1234 }
  ],
  "entrypoint": "com.agentfactory.generated.Application",
  "runCmd": "./gradlew bootRun",
  "openapiPath": "openapi.yaml",
  "envSample": ".env.sample"
}
```

## 스택 (고정)

- **언어**: Kotlin. Java 금지.
- **빌드**: Gradle + Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`).
- **프레임워크**: Spring Boot 3 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
- **DB**: PostgreSQL + Flyway (`flyway-core`, `flyway-database-postgresql`).
- **문서**: springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`).
- **DTO**: `data class` + jakarta.validation 애너테이션.
- **ID**: `UUID`. `@Id @GeneratedValue(strategy = GenerationType.UUID)` (JPA 3.1+ 표준). 애플리케이션 측에서 UUID를 만들어 INSERT에 포함시키므로 `save()` 호출 직후 반환된 엔티티는 완전 상태이며, DB의 `DEFAULT gen_random_uuid()` 는 안전망으로만 유지한다.
- **감사 필드**: `createdAt`, `updatedAt` 은 **Spring Data JPA Auditing** 으로 자동 관리. 공통 `@MappedSuperclass BaseEntity` 로 추출하고 모든 도메인 엔티티가 상속. `@CreatedDate`, `@LastModifiedDate` 애너테이션 + `@EntityListeners(AuditingEntityListener::class)`. `Application` 또는 별도 `@Configuration` 에 `@EnableJpaAuditing` 필수 (없으면 자동 채움 동작 안 함).
- **감사 필드 타입 = `OffsetDateTime` 일 때 DateTimeProvider 필수**: Spring Data Auditing 의 기본 `DateTimeProvider` 는 `LocalDateTime` 만 반환해서 `OffsetDateTime` 필드에 쓰려 하면 런타임 `IllegalArgumentException: Cannot convert unsupported date type java.time.LocalDateTime to java.time.OffsetDateTime` 로 **첫 INSERT 시 크래시** (이전 drift 로 확인됨). 반드시 `@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")` 로 참조하고 `@Bean fun auditingDateTimeProvider(): DateTimeProvider = DateTimeProvider { Optional.of(OffsetDateTime.now(ZoneOffset.UTC)) }` 를 `Application` 또는 `@Configuration` 에 추가.
- **네이밍**: 컬럼 매핑은 `@Column(name = "snake_case")`.

## 필수 산출물

```
backend/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  gradle/wrapper/gradle-wrapper.properties   # distributionUrl 과 gradle 버전
  .env.sample
  openapi.yaml                         # springdoc가 런타임 생성하므로 경로만 명시
  src/main/resources/application.yml
  src/main/resources/db/migration/V001__init.sql   # SchemaPlan.migration.upSql 그대로
  src/main/kotlin/com/agentfactory/generated/
    Application.kt                     # @SpringBootApplication + @EnableJpaAuditing
    common/
      BaseEntity.kt                    # @MappedSuperclass + @EntityListeners(AuditingEntityListener::class)
                                       # createdAt (@CreatedDate, updatable=false), updatedAt (@LastModifiedDate)
                                       # id: UUID? = null, @Id @GeneratedValue(strategy = GenerationType.UUID)
    config/                            # CORS, OpenAPI, Security, GlobalExceptionHandler
    <entity>/
      <Entity>.kt                      # : BaseEntity() — id/createdAt/updatedAt 를 다시 선언하지 않는다
      <Entity>Repository.kt            # JpaRepository
      <Entity>Service.kt               # 트랜잭션 경계
      <Entity>Controller.kt            # REST 컨트롤러
      dto/
        <Entity>CreateRequest.kt
        <Entity>UpdateRequest.kt
        <Entity>Response.kt
```

## API 규약

- 베이스 경로: `/api`.
- 엔티티당 CRUD: `GET /api/<resources>` (page, size, q), `GET /:id`, `POST /`, `PUT /:id`, `DELETE /:id`.
- 페이지네이션: `Pageable` → `Page<Response>` JSON (`content`, `totalElements`, `page`, `size`).
- 검색: 엔티티의 `string|text` 필드에 대한 단순 `ilike %q%` 필터 지원.
- 에러: `@ControllerAdvice`로 `MethodArgumentNotValidException`, `EntityNotFoundException` → 일관된 JSON (`{ code, message, fields }`).

## 지침

- 모든 파일 경로는 `workspaceDir` 기준 **상대경로**로만 기록. 절대경로 쓰지 않는다.
- Flyway 마이그레이션은 **`SchemaPlan.migration.upSql`을 그대로** 복사. 재해석 금지.
- Spring Data JPA의 `CrudRepository`가 아닌 `JpaRepository`를 쓴다 (페이징 필요).
- 모든 도메인 엔티티는 `common.BaseEntity` 를 상속. `id`/`createdAt`/`updatedAt` 은 BaseEntity 에 한 번만 정의하고 자식에서 재선언하지 않는다.
- `Application` 클래스에 `@EnableJpaAuditing` 을 부여 (없으면 `@CreatedDate`/`@LastModifiedDate` 동작 안 함 — 조용한 실패).
- `service.create()` / `update()` 는 `repository.save()` 반환값을 그대로 Response 로 매핑한다. **`save()` 후 재조회(reload) 패턴을 쓰지 않는다** — `GenerationType.UUID` + Auditing 으로 반환 엔티티가 이미 완전하다.
- 인증: `MvpSpec.auth.model == "single-admin"`이면 HTTP Basic + 단일 계정(`application.yml`에 `admin.username`, `admin.password`). `multi-role`이면 스텁만 남기고 TODO가 아니라 주석으로 "향후 JWT 도입 지점" 표기 대신 **구현 범위에서 제외하고 README에 명시**.
- **DB 연결 정보 분리**: `application.yml` 의 JDBC URL 은 **조각 환경변수** 로 조립. Docker compose 가 `DB_HOST=postgres` 만 바꿀 수 있게:
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mvpdb}
      username: ${DB_USERNAME:postgres}
      password: ${DB_PASSWORD:postgres}
  ```
  전체 URL을 `DB_URL` 하나로 주지 말 것 — host 만 바꾸는 유즈케이스가 깨진다.
- `openapi.yaml`은 직접 쓰지 않는다 — springdoc가 `/v3/api-docs`에서 제공. 대신 README에 확인 방법을 적는다.
- README.md에 실행법(환경변수, `./gradlew bootRun`, `docker compose up postgres`) 명시.
- **Gradle 래퍼 부트스트랩**: `gradle-wrapper.jar` 바이너리와 `gradlew` / `gradlew.bat` 스크립트는 **에이전트가 만들지 않는다** (바이너리 생성 불가). 대신 `gradle/wrapper/gradle-wrapper.properties` 파일에 `distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip` (또는 최신 호환 버전) 을 기록하고, README.md 에 "**최초 1회** 시스템에 `gradle` 설치 후 프로젝트 루트에서 `gradle wrapper --gradle-version 8.10 --distribution-type bin` 실행 — 이후부터는 `./gradlew` 사용 가능" 을 명시. 시스템 `gradle` 요구 사항도 README 전제 조건 섹션에 포함.
- **버전 호환성 매트릭스 (필수 pin)**: 아래 조합 중 하나를 사용. 섞지 말 것.
  - Kotlin **2.1.20+** + Spring Boot **3.3.x** + Gradle **8.10+ / 9.x** + JDK **17 또는 21**.
  - **금지**: Kotlin 1.9.x 는 Gradle 9.x 와 비호환 (`DefaultTaskCollection#configureEach` 에러). Gradle 8.x 만 사용하거나 Kotlin 2.1+ 로 이동.
- **JDK toolchain 정책**: `java { toolchain { languageVersion.set(JavaLanguageVersion.of(N)) } }` 는 N이 시스템에 설치돼 있어야 실행됨. 두 가지 옵션 중 하나를 따른다:
  1. toolchain 블록을 쓰되 `settings.gradle.kts` 에 **Foojay toolchain resolver 플러그인** (`id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"`) 을 추가해 자동 다운로드 허용.
  2. 또는 toolchain 블록을 **제거** 하고 시스템 JDK 를 그대로 사용. 이 경우 `jvmTarget` 과 일치시키고 README 에 필요한 JDK 버전 명시.
- **엔티티당 표준 CRUD 엔드포인트 범위 엄격 준수**: `MvpSpec.apis[*]` 에 **명시된 메서드+경로 조합만** 컨트롤러에 구현. 엔티티를 보고 "보통 이런 CRUD 가 있겠지" 라는 관례적 추정으로 spec 밖 엔드포인트(예: 스펙에 generate 만 있는데 plain POST create 도 추가) 를 만들지 않는다. qa-reviewer 가 이 차이를 잡는다.

## 하지 말 것

- 반쪽 스텁, "TODO 구현 예정" 주석. 범위에 없으면 파일 자체를 만들지 않는다.
- `MvpSpec`/`SchemaPlan`에 없는 엔티티/필드/API 추가.
- `application.yml`에 실제 비밀값. 모두 `${ENV_VAR:default}` 형태로.
- `!!` (강제 언랩) 사용. 불가피하면 한 줄 주석.
- 엔티티 클래스에 `id`/`createdAt`/`updatedAt` 직접 선언 (BaseEntity 상속 사용).
- `save()` 후 재조회해서 반환. Auditing + `GenerationType.UUID` 로 불필요.
- `insertable = false` 를 `id` 컬럼에 부여해 "DB가 id 생성" 패턴을 쓰려 하기 (Hibernate 가 null id 로 INSERT 시도해 실패 — 이전 drift 로 확인됨).
