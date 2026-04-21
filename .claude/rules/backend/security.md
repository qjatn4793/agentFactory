# backend/security

민감값(비밀번호·쿠키·외부 API 토큰 등)을 다루는 방침. 저장·전송·출력 모두 포함.

## 저장

- **비밀번호 해시**: BCrypt (`BCryptPasswordEncoder`). NoOp / 평문 저장 금지.
- **대칭 암호화 (AES-256-GCM)** — 쿠키·토큰·외부 자격증명처럼 **원문 복원이 필요한 민감값**:
  - 키: 환경변수 `APP_CRYPTO_KEY` (base64 encoded 32 bytes). 애플리케이션 부팅 시 없으면 **기동 실패**.
  - 저장 포맷: `iv(12B) || ciphertext || tag(16B)` 단일 `VARBINARY` 컬럼. 별도 IV 컬럼 만들지 않음.
  - 유틸: `infrastructure/crypto/CryptoService.kt` (encrypt(plain): ByteArray, decrypt(bytes): String).
- **해시값 (복원 불필요)**: SHA-256. 예: refresh token hash.

## 응답 · 출력

- API 응답 DTO 에 **원문 민감값 필드 금지**. `passwordHash`, `sessionId`, `cookies`, `rawToken` 등 절대 포함 불가.
- 대신 **마스킹 프리뷰** 필드 제공:
  - `<field>Preview: String?` — 앞 4자 + … + 뒤 4자 (예: `sessionIdPreview = "sess…abcd"`).
  - `has<Field>: Boolean` — 값 유무.
- 응답에 민감값 들어가면 qa-reviewer 가 error 로 잡는다 (신규 규칙).

## UI

- 비밀번호·쿠키 입력 필드는 **`type="password"`** 필수.
- "보기/숨기기 토글" 아이콘은 사용자 본인이 방금 입력한 값만 대상으로 허용. **서버에서 가져온 값을 UI 에 평문으로 노출 금지**.
- 비밀번호·토큰 1회 표시(임시 발급 등)는 발급 **직후 한 번만** 허용. 이후 재조회 불가.
- 관리자 화면에서도 저장된 쿠키·토큰 원문을 **복원해 표시하지 않는다**. 교체는 새 값 입력으로만.

## 감사 로그

- `AuditLog.diff` JSON 에 원문 민감값 넣지 않는다.
- "쿠키 갱신" 같은 이벤트는 action 과 target 만 기록. `oldValue`/`newValue` 같은 필드 금지.

## 환경변수 / 설정

- `application.yml` 은 `${ENV_VAR:default}` 만. 실제 값 하드코딩 금지.
- `.env.docker` 에 실제 값. 이 파일은 `.gitignore` 필수.
- 부팅 시 필수 시크릿 부재 → **기동 실패** + 로그에 누락 변수 이름 표시 (값 로그 금지).

## 재발 방지 체크 (PR·qa-reviewer 기준)

- 신규 `*Response` DTO 에 `password`, `hash`, `token`, `secret`, `cookie`, `sessionId` 등 문자열 필드가 있으면 경고.
- 신규 Input 요소에 `register("password")` 또는 `register("...[Pp]assword")` 패턴이 있는데 `type="password"` 없으면 error.
- `application.yml` 에 콜론 뒤 기본값으로 실제 비밀번호 문자열이 보이면 error.

## 하지 말 것

- 로그에 민감값 출력 (`log.info { "sessionId=$sessionId" }` 등).
- Exception 메시지·스택 트레이스에 민감값 포함.
- `toString()`·`equals` 자동 생성 (data class) 이 민감값을 노출 — 해당 필드는 `@JsonIgnore` + 수동 `toString()` 또는 masked 값만 노출.
- `localStorage` 에 토큰·쿠키 저장 (XSS 탈취 위험). 메모리 또는 HttpOnly 쿠키만.
- 비밀번호·쿠키를 이메일·Slack 으로 평문 전송.
