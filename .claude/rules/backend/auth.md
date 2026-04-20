# backend/auth

JWT 기반 인증 + Role 기반 접근 제어(RBAC). WebFlux Security 패턴. storelink6 파트너스 API 인증 체계를 단순화한 버전.

## 토큰 정책

- **Access token**: JWT · **15분**. stateless. 각 요청 `Authorization: Bearer <token>`.
- **Refresh token**: opaque(랜덤 문자열 해시) · **30일**. DB 저장 (`refresh_tokens` 테이블). 로그아웃·사용자 비활성화 시 즉시 폐기 가능.
- Access token 은 DB 조회 없이 검증(서명만). Refresh 는 항상 DB 조회.
- 서명 알고리즘: **HS256** (단일 인스턴스 MVP 전제). 시크릿은 `application.yml` 의 `${JWT_SECRET}` 환경변수. 최소 32바이트 랜덤값.

### JWT Claims (표준 최소)

```
{ "sub": "<userId>", "role": "ADMIN|EDITOR|VIEWER", "iat": ..., "exp": ..., "iss": "<app>" }
```

email/username 은 claim 에 넣지 않는다 — 변경 시 토큰 invalidation 부담. 필요하면 `/api/auth/me` 로 조회.

## Role 기반 접근 제어

- 역할: `ADMIN` / `EDITOR` / `VIEWER` (enum).
- Spring Security authority 표기: `"ROLE_ADMIN"` 처럼 `ROLE_` 접두사 필수 (Spring 관례).
- 접근 규칙은 `SecurityWebFilterChain` 의 `.authorizeExchange()` 에서 명시. 컨트롤러 메서드별 `@PreAuthorize` 는 최소 사용 (중앙 집중 선호).

```kotlin
// application/api/config/SecurityConfig.kt — 핵심 룰
http
    .authorizeExchange { ex -> ex
        .pathMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
        .pathMatchers(HttpMethod.GET, "/v3/api-docs/**", "/swagger-ui/**").permitAll()
        .pathMatchers("/api/users/**", "/api/audit-logs/**").hasRole("ADMIN")
        .pathMatchers(HttpMethod.POST,   "/api/**").hasAnyRole("ADMIN", "EDITOR")
        .pathMatchers(HttpMethod.PUT,    "/api/**").hasAnyRole("ADMIN", "EDITOR")
        .pathMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN", "EDITOR")
        .pathMatchers(HttpMethod.GET,    "/api/**").authenticated()     // ADMIN/EDITOR/VIEWER 전부
        .anyExchange().authenticated()
    }
```

- **403 우선 원칙**: 인증되었지만 권한 없으면 403, 인증 안 되었으면 401. `.accessDeniedHandler` 와 `.authenticationEntryPoint` 명시.

## 비밀번호

- **BCrypt** (`BCryptPasswordEncoder`). NoOpPasswordEncoder 금지.
- 저장 컬럼: `password_hash VARCHAR(255)`. 평문 저장 금지.
- 최초 로그인 / ADMIN 이 리셋한 경우: `must_change_password BOOLEAN` 플래그로 강제 변경 흐름.
- 비밀번호 정책(MVP): 8자 이상. 복잡도 룰은 추후 강화.

## 부팅 시 ADMIN 시드

- 애플리케이션 시작 후 `ApplicationRunner` 가 `users` 테이블에 ADMIN 계정이 하나도 없으면, 환경변수 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 로 시드 생성.
- 이미 ADMIN 이 있으면 생성 건너뜀 (로그만 남김).
- `must_change_password = true` 로 생성 — 사용자가 최초 로그인 후 강제 변경.
- 환경변수가 비어 있으면 시드하지 않고 경고 로그 + 기동 실패 방지(서버는 뜨되 로그인 불가 상태).

## Refresh 토큰 관리

- `refresh_tokens` 테이블: `id`, `user_id`, `token_hash` (저장은 해시만 — 탈취 시 원문 복원 불가), `expires_at`, `revoked_at`, `created_at`, `user_agent`, `client_ip`.
- 로그인 시 원문 refresh token 을 클라이언트에 1회 반환, DB 에는 SHA-256 해시만 저장.
- Refresh API: 해시로 조회 → `revoked_at IS NULL AND expires_at > NOW()` → 새 access + (옵션) 새 refresh 발급.
- 로그아웃: 해당 refresh token `revoked_at = NOW()`.
- 사용자 비활성화(`deactivate`): 해당 user 의 모든 refresh `revoked_at = NOW()` 일괄 업데이트.
- 만료 refresh 하드 삭제: `@Scheduled(cron = "0 0 3 * * *")` 배치로 `expires_at < NOW() - 7일` 삭제.

## 감사 로그

- 별도 파일: `rules/backend/auth.md` 에서는 **인증 이벤트만** 다룸.
- 도메인 변경 감사는 `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)` 로 발행, `AuditLogAdaptor` 가 수신해 DB 기록.
- 인증 이벤트 (LOGIN_SUCCESS / LOGIN_FAILED / LOGOUT / PASSWORD_CHANGED / USER_DEACTIVATED): 컨트롤러 또는 서비스에서 직접 이벤트 발행.
- 기록 항목: `actor_user_id`(nullable — 익명 로그인 실패), `action`, `target_type`, `target_id`, `diff`(JSON, optional), `ip`, `user_agent`, `created_at`.

## WebFlux 특유 주의 사항

- `ReactiveAuthenticationManager` 사용. `AuthenticationManager` 아님.
- `SecurityContextHolder` **아님** → `ReactiveSecurityContextHolder.getContext()` 로 `Mono<SecurityContext>` 획득.
- 컨트롤러에서 현재 사용자 조회: `@AuthenticationPrincipal` 또는 `ReactiveSecurityContextHolder.getContext().awaitSingle().authentication.principal`.
- JWT 필터는 `WebFilter` 또는 `AuthenticationWebFilter` + `ServerAuthenticationConverter` 조합.

## 감사 로그 자동 생성 대상 (도메인)

- 모든 Adaptor 의 INSERT / UPDATE / DELETE 직후 이벤트 발행.
- Publisher 는 domain 모듈에 두지 않음 (Spring 의존 금지) → infrastructure 의 Adaptor 에서 발행.
- 이벤트 → `ApplicationEventPublisher.publishEvent(DomainAuditEvent(...))`.
- Listener (`@Component @TransactionalEventListener(phase = AFTER_COMMIT)`) 가 `AuditLog` 로 변환해 저장.

## 하지 말 것

- Access token 을 localStorage 에 저장 (XSS 탈취 위험). sessionStorage 도 탭 간 공유됨 — **메모리(클로저/React state)** 에만.
- Refresh token 을 localStorage 저장. HttpOnly 쿠키가 이상적이지만 MVP 에서 단순화하여 **sessionStorage** 사용 (같은 도메인 전제).
- 모든 요청에 Access token 갱신 (every-request rotation) — MVP 범위 초과.
- 로그아웃 시 DB 폐기 건너뛰기 — 탈취된 토큰이 15분간 유효해진다.
- 컨트롤러마다 `@PreAuthorize` 산발 — `SecurityConfig` 중앙 룰 기본, 예외적 필요 시만 메서드 애너테이션.
