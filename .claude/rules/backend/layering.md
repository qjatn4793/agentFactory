# backend/layering

헥사고날 아키텍처 기반 멀티모듈 레이아웃. agentFactory 자체 백엔드와 생성되는 MVP 백엔드 모두에 적용.

## 모듈 구성 (기본 3개)

```
backend/
├── domain/                     — 순수 Kotlin. 비즈니스 로직과 포트 정의.
├── infrastructure/             — Spring / R2DBC / 외부 어댑터 구현.
└── application/
    └── api/                    — 실행 진입점 (main). Controller · DTO · Config.
```

필요 시 `external/` (범용 어댑터: jwt, storage, notification), `core/` (공통 유틸·예외 인터페이스) 모듈을 추가. MVP 에서는 기본 3개만.

## 의존 방향 (절대 규칙)

```
application ──► domain
application ──► infrastructure
infrastructure ──► domain     (포트 구현)
domain ──► (아무것도 의존하지 않음 — 순수 Kotlin only)
```

- `infrastructure` 는 `domain` 에만 의존. `application` 에 역방향 의존 금지.
- `domain` 은 Spring / JPA / R2DBC / Jackson / Flyway 전부 의존 금지 — `rules/backend/domain-purity.md` 참조.
- `application` 이 `domain` 과 `infrastructure` 둘 다 의존해 실행 컨텍스트를 조립.

## 각 모듈의 책임

### domain

- **command/**: 도메인 입력 (data class, `*Command`).
- **query/**: 도메인 조회 객체 (`*Query`, `*FindAllBySearchPagedQuery`, `*CountBySearchQuery`).
- **model/**: 도메인 모델 (data class, 불변 선호).
- **service/**: 비즈니스 로직 (`*Service`, suspend 함수).
- **persistence/**: 포트 인터페이스 (`I*Port`) — **인터페이스만**, 구현 없음.
- **external/**: 외부 시스템 포트 (`I*Port`) — 인터페이스만.
- **enums/**: 비즈니스 열거형.
- **exception/**: 도메인 예외 (`*Exception` + `ErrorCode` enum).
- **aggregate/** (선택): DDD 집합체 — 여러 도메인 서비스 오케스트레이션 (`*AggregateService`, `*ViewAggregate`).

### infrastructure

- **persistence/entity/**: R2DBC / JPA 엔티티 (`*Entity`, `@Table` 명시).
- **persistence/repository/r2dbc/**: R2DBC 리포지토리 (`*Repository`, `ReactiveCrudRepository`).
- **persistence/repository/jooq/** (옵션): JOOQ 복합 쿼리 (`*JooqRepository`).
- **persistence/adaptor/**: 포트 구현 (`*Adaptor`, `@Component`).
- **external/**: 외부 시스템 어댑터 구현 (`*Adaptor`).
- **config/**: R2DBC / Flyway / Redisson 등 infrastructure bean 설정.

### application/api

- **Application.kt**: `@SpringBootApplication` + main.
- **presentation/controller/**: REST 컨트롤러 (`*Controller`).
- **presentation/dto/**: API DTO (`*Request`, `*Response`).
- **config/**: WebFlux / Security / OpenAPI / GlobalErrorAttributes / CORS.
- **resources/**: `application.yml`, `db/migration/V*.sql`.

## 패키지 루트

생성되는 MVP 는 `com.agentfactory.generated` 루트. agentFactory 자체 백엔드는 `io.agentfactory` 루트.

## Gradle 간 의존 선언

```kotlin
// infrastructure/build.gradle.kts
dependencies {
    implementation(project(":domain"))
    // + Spring / R2DBC / JOOQ
}

// application/api/build.gradle.kts
dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))
    // + spring-boot-starter-webflux / security / springdoc
}

// domain/build.gradle.kts
dependencies {
    // Spring / JPA / R2DBC / Jackson 의존 금지
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}
```

## 하지 말 것

- 단일 모듈에 패키지만 나눠 "멀티모듈 같은 느낌"을 내기 — 순수성 강제가 약해진다.
- `domain` 에서 Spring bean 생성 / `@Component` / `@Service` 사용.
- `application` 에서 `infrastructure` 의 내부 구현 클래스를 직접 import (어댑터는 port 를 통해 주입).
- 순환 의존. build 단계에서 gradle 이 실패한다.
