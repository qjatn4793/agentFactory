# backend/persistence

영속성 기본 전략. R2DBC-first, JPA 는 legacy 용도로만.

## 1. R2DBC (주)

- 드라이버: `io.asyncer:r2dbc-mysql:1.3.0`.
- 리포지토리: `org.springframework.data.repository.kotlin.CoroutineCrudRepository` 또는 `ReactiveCrudRepository`. MVP 는 Coroutine 버전 선호.
- 엔티티 (Persistable 구현 — JVM signature 충돌 회피 패턴):
  ```kotlin
  @Table("todos")
  data class TodoEntity(
      @Id
      @Column("id")
      @get:JvmName("getIdValue")                   // val id 의 Kotlin 자동 getter 이름을 getIdValue() 로 변경
      val id: String,                              // UUID CHAR(36)
      @Column("title") val title: String,
      @Column("created_at") val createdAt: OffsetDateTime? = null,
      @Column("updated_at") val updatedAt: OffsetDateTime? = null,
  ) : Persistable<String> {
      @Transient
      private var _isNew: Boolean = true

      override fun getId(): String = id            // Persistable 의 getId() 를 명시적으로 override
      override fun isNew(): Boolean = _isNew
      fun markNotNew(): TodoEntity = apply { _isNew = false }
  }
  ```
- **왜 `@get:JvmName("getIdValue")` 와 `override fun getId()` 가 둘 다 필요한가**:
  - Kotlin `val id: String` 은 기본적으로 `getId(): String` 자동 getter 를 생성.
  - `Persistable<String>` (Java 인터페이스) 은 `getId(): String?` (nullable) 을 요구.
  - 이 둘이 JVM 에서 동일 이름·다른 반환 타입을 가지려 하므로 컴파일 에러 2종 발생:
    1. `override val id: String` 로 바꾸면 → `'id' overrides nothing` (Java 인터페이스는 Kotlin property 가 아니므로 override 불가).
    2. `val id: String` + `override fun getId(): String = id` 만 두면 → `Platform declaration clash: getId()Ljava/lang/String;` (자동 getter 와 override 함수가 동일 signature).
  - 해결: `@get:JvmName("getIdValue")` 로 자동 getter 이름을 다른 것으로 바꾸고, `override fun getId()` 는 명시 구현. Kotlin 코드에서는 여전히 `entity.id` 로 접근 가능.
- R2DBC 는 JPA 의 `@GeneratedValue` 가 없다 → **애플리케이션에서 `UUID.randomUUID().toString()` 생성 후 주입**.
- Insert/Update 판정은 `Persistable<ID>` 구현으로 명시 (R2DBC 는 id 가 null 이 아니면 default 로 update 시도 — `isNew()` 를 반드시 제어).

## 2. JPA (보조)

- 백업 DB · 레거시 데이터 등 blocking 전용 시에만.
- R2DBC 와 **트랜잭션 혼용 금지**. 분리된 `DataSource` + 분리된 `TransactionManager`.
- MVP 는 **JPA 사용 안 함**이 기본.

## 3. JOOQ (옵션)

- 복합 조회가 필요할 때만. `build/generated/jooq` 코드 생성 단계 포함.
- MVP 는 기본 제외.

## 4. 마이그레이션 (Flyway)

- **Flyway 는 JDBC 경로로 실행** — R2DBC 로 마이그레이션 불가.
- `flyway-core` + `flyway-mysql` + `mysql-connector-j` 의존성.
- `spring.flyway.url`, `spring.flyway.user`, `spring.flyway.password` 를 별도로 주입 (R2DBC 연결과 독립).
  ```yaml
  spring:
    r2dbc:
      url: r2dbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
    flyway:
      url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
      user: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      locations: classpath:db/migration
      baseline-on-migrate: true
  ```
- 마이그레이션 파일: `application/api/src/main/resources/db/migration/V001__init.sql` (MVP 는 단일 파일 `V001__init.sql`).
- local 프로파일에서만 자동 실행하고 싶으면 `spring.flyway.enabled` 를 프로파일별로 제어.

## 5. 트랜잭션

- **트랜잭션 매니저 이름 명시 필수**:
  ```kotlin
  @Transactional(transactionManager = "r2dbcTransactionManager")
  suspend fun create(command: TodoInsertCommand): Todo { ... }
  ```
- 선언 위치: `infrastructure` 또는 `application` 레이어에서만. `domain` 에서는 금지.
- 프로그래밍 방식이 필요하면 `TransactionalOperator.create(reactiveTransactionManager)` 사용.
- `Propagation.REQUIRES_NEW` 가 필요한 경우 별도 `NewTransactionalInvoker` 컴포넌트 도입.

### Bean 설정 (예)

```kotlin
// infrastructure/config/R2dbcConfig.kt
@Configuration
@EnableR2dbcRepositories(basePackages = ["com.agentfactory.generated.infrastructure.persistence.repository.r2dbc"])
@EnableR2dbcAuditing
class R2dbcConfig {

    @Bean
    fun r2dbcTransactionManager(connectionFactory: ConnectionFactory): ReactiveTransactionManager =
        R2dbcTransactionManager(connectionFactory)

    @Bean
    fun auditorAware(): ReactiveAuditorAware<String> =
        ReactiveAuditorAware { Mono.just("system") }  // MVP 기본
}
```

## 6. Auditing

- `@EnableR2dbcAuditing`.
- Entity 의 `createdAt` / `updatedAt` 은 `@CreatedDate` / `@LastModifiedDate` + `OffsetDateTime`.
- **타입 주의**: `OffsetDateTime` 사용 시 기본 제공자가 `LocalDateTime` 을 반환하면 런타임 크래시 — `DateTimeProvider` bean 에서 `OffsetDateTime.now(ZoneOffset.UTC)` 반환하도록 명시.

## 7. Adaptor 패턴 (Port ↔ Entity 매핑)

```kotlin
// infrastructure/persistence/adaptor/todo/TodoAdaptor.kt
@Component
class TodoAdaptor(
    private val repository: TodoRepository,
) : ITodoPort {

    override suspend fun insert(command: TodoInsertCommand): Todo {
        val entity = TodoEntity(
            id = UUID.randomUUID().toString(),
            title = command.title,
            createdAt = OffsetDateTime.now(ZoneOffset.UTC),
            updatedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )
        val saved = repository.save(entity)
        return saved.toDomain()
    }
}

private fun TodoEntity.toDomain() = Todo(
    id = id, title = title,
    createdAt = createdAt, updatedAt = updatedAt,
)
```

- Domain 모델 ↔ Entity 매핑은 **Adaptor 안의 extension** 으로 둔다. 별도 mapper 클래스는 MVP 범위에서 과잉.

## 하지 말 것

- JPA 와 R2DBC 를 같은 트랜잭션 경계 안에서 섞기.
- `@Transactional` 을 `domain` 또는 `*Port` 인터페이스에 선언.
- R2DBC 엔티티의 `@Id` 필드를 nullable `UUID?` 로 선언하고 애플리케이션에서 null INSERT 시도 (R2DBC 는 이 경우 update 시도).
- Flyway migration upSql 을 agent 가 재작성 — `SchemaPlan.migration.upSql` 을 **바이트 단위로 그대로 복사**.
