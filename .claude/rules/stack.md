# stack rules

agentFactory의 스택은 고정이다. 회귀시키지 않는다.

## Frontend — TypeScript

- 언어: TypeScript. `.js`로 회귀시키지 않는다. 필요하면 `.ts` / `.tsx`로 승격한다.
- `any`는 마지막 수단. 불가피하면 왜 필요한지 한 줄 주석.
- 상태 관리·라우팅·UI 라이브러리 선택은 MVP 생성기가 산출하는 스펙에 따라 결정되며, 일관성을 유지한다 (한 MVP 내에서 혼용 금지).

## Backend — Kotlin

- 언어: Kotlin. Java로 회귀시키지 않는다. 기존 Java 상호운용이 필요한 경우에도 새 파일은 Kotlin으로 작성.
- null 안전성을 활용한다. `!!`는 최후의 수단. 불가피하면 한 줄 주석.
- 빌드 도구: Gradle (Kotlin DSL `build.gradle.kts` 선호).
- DTO는 `data class`, 도메인 모델은 sealed/abstract class 사용을 기본으로 고려.

## MVP 생성 경로

- Claude 호출은 **백엔드에서만** 한다. 프론트엔드에서 Claude API를 직접 호출하지 않는다 (키 노출 방지 + 중앙 관측).
- 생성은 장시간 실행될 수 있다. 동기 블로킹 API 대신 작업 큐 + 상태 폴링 또는 SSE/WebSocket 스트리밍을 기본 전제로 설계한다.
- 생성된 산출물은 프로젝트 트리에 직접 쓰지 않고 격리된 작업공간(예: `generated/<jobId>/`)에 작성한 뒤 결과로 반환한다.
