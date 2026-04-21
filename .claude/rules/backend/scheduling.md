# backend/scheduling

`@Scheduled` 기반 배치·주기 작업 컨벤션. 단일 인스턴스 전제 (rules/backend/stack.md 의 운영 전제 참조).

## 스케줄러 배치 위치

- `@Scheduled` 메서드는 `application/api/scheduler/*Scheduler.kt` 에 둔다. domain·infrastructure 에 두지 않는다.
- `@EnableScheduling` 은 `Application.kt` 또는 `SchedulerConfig.kt` 에 한 번만.

## 필수 규칙

### 1. 진입점은 blocking, 내부는 suspend

```kotlin
@Scheduled(cron = "0 0 */6 * * *")
fun onTick() {
    runBlocking { refreshAll() }
}

private suspend fun refreshAll() { ... }
```

- `@Scheduled` 메서드는 Spring 의 scheduler thread 가 호출 → coroutine 진입은 `runBlocking`.
- **suspend 메서드에 `@Scheduled` 직접 붙이지 않는다** (Spring 이 코루틴을 모름).

### 2. 실행 이력 DB 기록 필수

- `BatchRun` (또는 동급) 엔티티에 잡 실행 시작/종료/성공·실패 카운트·에러 메시지 기록.
- Spring Batch 의 JobRepository 를 흉내내는 최소 구현으로 관측성 확보.
- 실패 시 `errorMessage` 에 `e.message` + 주요 트레이스 200자 정도 포함. 전체 스택은 로그로.

### 3. cron 표기

- Spring cron: 6 필드 (초 분 시 일 월 요일).
- 개발 시 분 단위 테스트 필요하면 `application-local.yml` 로 cron 분리, prod 는 시간 단위.

### 4. 동시성 제어

- 단일 인스턴스 전제이므로 분산 락 불필요.
- JVM 내에서 동일 잡 중복 실행 방지는 다음 중 하나:
  - `@Scheduled(fixedDelay=...)` — 이전 실행 완료 후 N초 후 다음 실행 (중복 없음).
  - 또는 `DiscoveryJob`/`BatchRun` 같이 "실행 중인 행이 있으면 새 실행 skip" 패턴 (DB 유니크 인덱스로 경합 방지).
- 여러 인스턴스로 확장 시 MySQL `GET_LOCK()` 또는 Redis 기반 분산 락 도입 — 그 시점까지 미룸.

### 5. 외부 API 호출 시 rate limit

- Bucket4j (in-memory) 사용. 플랫폼별로 `PlatformRateLimiter` 같은 단일 빈에서 관리.
  ```kotlin
  @Component
  class PlatformRateLimiter {
      private val instagram = Bucket.builder()
          .addLimit(Bandwidth.classic(200, Refill.intervally(200, Duration.ofHours(1))))
          .build()
      suspend fun acquire(platform: Platform) {
          val bucket = pickBucket(platform)
          while (!bucket.tryConsume(1)) delay(100)
      }
  }
  ```
- Coroutines 동시성 상한: `Flow.flatMapMerge(concurrency = 4)` 또는 `Semaphore(permits = 4)`.

### 6. 재시도·타임아웃

- 외부 API 호출: **3회 고정 지연 3초 간격 + 전체 12초 타임아웃** (rules/backend/error-handling.md 와 일치).
- 재시도는 Reactor `retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3)))` 또는 coroutine 수동.
- 재시도 실패는 개별 항목 단위로 `failed += 1` 카운트하고 다음 항목으로 **계속 진행** (한 항목 실패로 배치 전체 중단 금지).

### 7. 실패 알림

- `BatchRun.status = FAILED` 로 저장되면 Discord/Slack 어댑터로 알림 (rules/backend/observability.md 추후 정의).
- MVP 단계에서는 `kotlin-logging` 으로 `logger.error` 만으로도 충분. 외부 통지는 관측성 도입 시.

## 템플릿 (참조용)

```kotlin
@Component
class XxxScheduler(
    private val service: XxxService,
    private val batchRunPort: IBatchRunPort,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 */6 * * *")
    fun onTick() {
        runBlocking { run() }
    }

    private suspend fun run() {
        val jobName = "XXX_JOB"
        val run = batchRunPort.start(jobName)
        var total = 0; var ok = 0; var fail = 0
        try {
            // ... 실제 작업. 각 항목마다 total++, ok++ 또는 fail++
            batchRunPort.finish(run.id, total, ok, fail)
        } catch (e: Throwable) {
            log.error(e) { "$jobName failed" }
            batchRunPort.fail(run.id, e.message ?: "unknown")
            throw e
        }
    }
}
```

## 하지 말 것

- `Thread.sleep` 를 스케줄러 안에서 사용 — Coroutines `delay` 사용.
- 한 배치 안에서 여러 플랫폼 호출을 직렬로 — `flatMapMerge` 또는 `coroutineScope` 로 병렬.
- cron 을 하드코딩 — 환경별 `application-{profile}.yml` 에서 오버라이드 가능하도록 `@Value("\${app.schedule.follower-refresh}")` 패턴.
- 배치 실행 도중 DB 트랜잭션을 전체 배치 하나로 묶기 — 항목 단위 트랜잭션 (실패 격리).
