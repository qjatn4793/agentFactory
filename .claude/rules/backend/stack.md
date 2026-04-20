# backend/stack

agentFactory 백엔드 및 MVP 생성물 백엔드의 기술 스택. 회귀시키지 않는다.

## 언어 · 런타임

- **Kotlin 2.1.20** (고정).
- **JDK 21**.
- Java 로 회귀 금지. 기존 Java 상호운용이 있어도 새 파일은 Kotlin.
- null 안전성 활용. `!!` 는 최후의 수단 — 불가피하면 한 줄 주석.

### Kotlin 버전 왜 고정인가

- **Kotlin 2.2.x 금지** (Gradle 8.10 과 incremental compilation 비호환). 증상: `Failed to transform kotlin-reflect-*.jar ... BuildToolsApiClasspathEntrySnapshotTransform ... ClasspathEntrySnapshotter$Settings` 빌드 오류로 infrastructure:compileKotlin 실패.
- Kotlin 2.2.x 로 가려면 Gradle **9.x 필수**. 하지만 이 프로젝트는 Gradle 8.10 으로 고정 (MVP 빌드 시간·툴체인 안정성). 따라서 Kotlin 도 2.1.20 고정.
- 2.1.x 이하로 내리지 말 것 (Kotlin 1.9.x 는 Gradle 9.x 와 비호환, 2.0.x 는 Spring Boot 3.3.x 호환 이슈 존재).

## 빌드

- **Gradle 8.10+ / 9.x** + Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`).
- 멀티모듈 구성. 루트에 공통 설정 + `subprojects` 블록.
- Gradle wrapper 는 `gradle wrapper --gradle-version 8.10 --distribution-type bin` 으로 한 번 생성 후 커밋.

## 프레임워크

- **Spring Boot 3.3.7**.
- Web 스택: **spring-boot-starter-webflux** (MVC 금지). Coroutines 로 suspend 함수 우선.
- DI: Spring. 단, `domain` 모듈은 Spring 의존 금지 — `rules/backend/domain-purity.md` 참조.
- **`application/api` 모듈에 `org.springframework:spring-tx` 를 implementation 으로 명시** — `@Transactional` 애너테이션을 컨트롤러에서 사용하므로 필수. webflux starter 만으로는 transitive 하게 가져오지 않아 `Unresolved reference 'Transactional'` 컴파일 에러 발생.

## 비동기 · 동시성

- **kotlinx-coroutines-reactor** 1.8.1+. suspend 함수와 `Flow` 기본.
- blocking 호출은 `Dispatchers.IO` 로 명시적 오프로딩.

## 영속성

- **주**: R2DBC (MySQL) — `spring-boot-starter-data-r2dbc` + `io.asyncer:r2dbc-mysql:1.3.0`.
- **보조**: JOOQ 3.19.x — 복합 조회 쿼리 필요 시 옵션. 기본은 제외.
- **마이그레이션**: Flyway 10.x — `flyway-core` + `flyway-mysql`. Flyway 는 JDBC 경로로 실행 (드라이버: `mysql-connector-j`).
- 상세: `rules/backend/persistence.md`.

## DB

- **MySQL 8.0+**. `utf8mb4` / `utf8mb4_unicode_ci`.
- 스키마 방언 상세: `schema-designer` 에이전트 + `rules/backend/persistence.md`.

## DTO

- `data class` + `jakarta.validation` 애너테이션.
- **API 레이어**: `*Request`, `*Response`.
- **도메인 입력**: `*Command`.
- **도메인 조회**: `*Query`.
- 명명 규칙 상세: `rules/backend/naming.md`.

## 문서화

- **springdoc-openapi 2.6.0** (`springdoc-openapi-starter-webflux-ui`).

## 로깅

- **kotlin-logging 7.0.3** (`io.github.oshai:kotlin-logging-jvm`).
- logback 설정은 공통 모듈(infrastructure 의 하위 또는 application 리소스) 로 공유.

## 테스트

- **Kotest 5.9.1** (`BehaviorSpec` 권장, `FunSpec` 선택).
- **MockK 1.13.13**. 코루틴은 `coEvery`/`coVerify`.
- JUnit 5 Runner.
- 단위: `domain` 레이어 + MockK 로 port 모킹.
- 통합: `infrastructure` 레이어 — 실제 MySQL 연결.

## 보안

- Spring Security WebFlux. JWT 또는 단일 admin 계정(MVP 기본).
- 비밀번호는 BCrypt (NoOp 금지, 평문 저장 금지).
- 민감값은 `application.yml` 에 하드코딩하지 않는다 — `${ENV_VAR:default}` 형태.

## 버전 호환성 매트릭스 (필수 pin — 섞지 말 것)

- **Kotlin 2.1.20** + Spring Boot **3.3.7** + Gradle **8.10** + JDK **21** — 이 조합으로 고정.
- **금지 조합**:
  - Kotlin 2.2.x + Gradle 8.10 → `BuildToolsApiClasspathEntrySnapshotTransform` 빌드 실패 (위 "Kotlin 버전 왜 고정인가" 참조).
  - Kotlin 1.9.x + Gradle 9.x → `DefaultTaskCollection#configureEach` 에러.
  - Gradle 9.x 로 올리고 싶다면 Kotlin 도 2.2.x 로 동기 업그레이드 — 하나만 바꾸지 말 것.

## Claude 호출 경로

- Claude API / Agent SDK 호출은 **백엔드에서만** 한다 (키 노출 방지 + 중앙 관측).
- 생성은 장시간 실행. 동기 블로킹 대신 작업 큐 + 상태 폴링 또는 SSE/WebSocket 스트리밍.
- 산출물은 프로젝트 트리에 직접 쓰지 않고 격리된 작업공간(`generated/<jobId>/`) 에 작성 후 반환.
