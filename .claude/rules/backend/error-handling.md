# backend/error-handling

중앙 에러 처리 · 도메인 예외 · 응답 포맷. storelink6 의 `IExceptionHandler<T>` 플러그인 패턴을 그대로 적용.

## 1. 응답 포맷 (고정)

```json
{
  "status": 400,
  "messageCode": "TODO_INVALID_STATUS",
  "message": "사용자에게 보여줄 메시지"
}
```

- 스택 트레이스, DB 에러 원문, 내부 경로 등 **민감 정보 포함 금지**.
- 검증 실패 (`MethodArgumentNotValidException` / WebFlux 의 `WebExchangeBindException`) 시에는 `fields` 배열 추가:
  ```json
  { "status": 400, "messageCode": "VALIDATION_FAILED",
    "message": "입력값이 올바르지 않습니다.",
    "fields": [ { "field": "title", "message": "must not be blank" } ] }
  ```

## 2. `IExceptionHandler<T>` 플러그인 인터페이스 (core)

```kotlin
// domain/common/exception/IExceptionHandler.kt  — core 모듈이 있으면 거기에.
interface IExceptionHandler<out T : Throwable> {
    val exceptionType: KClass<out T>
    fun supports(throwable: Throwable): Boolean =
        exceptionType.isInstance(throwable)
    fun handle(exception: Throwable, attributes: MutableMap<String, Any?>)
}
```

## 3. 도메인 예외 패턴

```kotlin
// domain/common/exception/AgentFactoryException.kt
abstract class AgentFactoryException(
    val errorCode: ErrorCode,
    val params: Map<String, Any?> = emptyMap(),
) : RuntimeException(errorCode.defaultMessage)

enum class ErrorCode(val status: Int, val defaultMessage: String) {
    TODO_NOT_FOUND(404, "해당 할 일을 찾을 수 없습니다."),
    TODO_INVALID_STATUS(400, "유효하지 않은 상태입니다."),
    VALIDATION_FAILED(400, "입력값이 올바르지 않습니다."),
}
```

- 도메인별로 하위 예외 확장 (`TodoException(errorCode)`). 컨트롤러에서 직접 잡지 않는다.
- 예외 생성 시 `params` 에 사용자에게 보여도 되는 맥락만 담는다.

## 4. GlobalErrorAttributes (WebFlux)

```kotlin
// application/api/config/GlobalErrorAttributes.kt
@Component
class GlobalErrorAttributes(
    private val handlers: List<IExceptionHandler<out Throwable>>,
) : DefaultErrorAttributes() {

    override fun getErrorAttributes(
        request: ServerRequest,
        options: ErrorAttributeOptions,
    ): MutableMap<String, Any?> {
        val attributes = super.getErrorAttributes(request, options)
        val throwable = getError(request) ?: return attributes
        val handler = handlers.firstOrNull { it.supports(throwable) }
        handler?.handle(throwable, attributes)
        return attributes
    }
}
```

## 5. 구체 핸들러 등록

- 서비스(또는 MVP) 마다 필요한 핸들러를 `@Component` 로 등록:
  - `AgentFactoryExceptionHandler` (도메인 공통 예외)
  - `ValidationExceptionHandler` (`WebExchangeBindException`, `ConstraintViolationException`)
  - `ReactiveTransactionExceptionHandler` (`CannotCreateTransactionException`)
  - `FallbackExceptionHandler` (마지막 안전망, 500)

## 6. 재시도 전략 (외부 API 호출)

- `WebClient` 또는 `HttpInterfaceClient` 호출은 **3회 고정 지연, 3초 간격** + `Duration.ofSeconds(12)` 전체 타임아웃.
- Reactor: `retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3)))`.
- 분산 락(`Redisson`) 획득 실패는 도메인 예외로 변환 후 상위에서 재시도 루프 구성 — 인프라 예외를 그대로 응답에 노출하지 않는다.

## 7. 로깅

- `kotlin-logging` 사용. 예외 발생 지점에서 **구조화 로그** (`logger.warn { ... }`, `logger.error(throwable) { ... }`).
- 사용자 ID / jobId / traceId 등 관측 식별자를 MDC 또는 structured key 로 포함.
- 예외 스택 트레이스는 로그에만, 응답에는 절대 포함 금지.

## 하지 말 것

- 컨트롤러마다 try-catch 로 예외 메시지 하드코딩. 반드시 `IExceptionHandler` 로 집중.
- `@ControllerAdvice` + `@ExceptionHandler` (MVC 패턴) — WebFlux 에서는 `GlobalErrorAttributes` 로 통일.
- DB 드라이버 원문 메시지를 사용자에게 노출 (`SQLException.message`).
- 4xx/5xx 구분 없이 모두 500 으로 떨어뜨리기.
