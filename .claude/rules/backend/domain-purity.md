# backend/domain-purity

`domain` 모듈은 순수 Kotlin 만. 프레임워크 의존 금지.

## 금지 의존 (domain/build.gradle.kts)

- Spring (모든 `spring-*`, `spring-boot-*`)
- JPA (`jakarta.persistence:*`, `hibernate-*`)
- R2DBC (`spring-data-r2dbc`, `io.asyncer:r2dbc-*`)
- Jackson (`com.fasterxml.jackson.*`) — DTO 직렬화는 application 책임
- Flyway
- Logback / SLF4J binding (interface `org.slf4j:slf4j-api` 까지는 허용)
- Validation (`jakarta.validation:*`) — 입력 검증은 `*Request` (application) 에서
- HTTP 클라이언트 / WebClient

## 허용 의존

```kotlin
// domain/build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // 필요 시 coroutines-reactor 정도까지만
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.13")
}
```

## 코드 레벨 금지 사항

- `@Service`, `@Component`, `@Repository`, `@Transactional` — domain 에서 선언 금지. 서비스는 일반 `class`, DI 는 infrastructure 의 `@Configuration` 에서 `@Bean` 으로 등록.
- `@Entity`, `@Table`, `@Id` 등 JPA/R2DBC 애너테이션 — entity 는 infrastructure 에.
- `@JsonProperty`, `@JsonIgnore` — 도메인 모델을 API 에 직접 노출 금지. `*Response` 로 매핑.

## 왜

- 도메인 로직이 프레임워크 업그레이드에 흔들리지 않는다.
- 단위 테스트가 빠르다 (Spring context 불필요).
- 도메인 모델을 다른 입출력 채널 (배치, 이벤트 리스너, CLI) 에서 재사용 가능.

## 서비스의 DI (domain 에서는 생성자만)

```kotlin
// domain/service/todo/TodoService.kt  —  Spring 애너테이션 없음
class TodoService(
    private val todoPort: ITodoPort,
    private val categoryPort: ICategoryPort
) {
    suspend fun create(command: TodoInsertCommand): Todo { ... }
}
```

```kotlin
// infrastructure/config/DomainServiceConfig.kt  —  bean 등록
@Configuration
class DomainServiceConfig {
    @Bean
    fun todoService(
        todoPort: ITodoPort,
        categoryPort: ICategoryPort,
    ) = TodoService(todoPort, categoryPort)
}
```

## 검증

- `qa-reviewer` 는 `domain/**/*.kt` 에 `import org.springframework.*` / `import jakarta.persistence.*` / `import org.flywaydb.*` 가 있으면 fail.
