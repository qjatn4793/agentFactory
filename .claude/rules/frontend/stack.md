# frontend/stack

agentFactory 프론트엔드 스택 규칙. 회귀시키지 않는다.

## 언어

- **TypeScript** 고정. `.js` 로 회귀 금지. 필요하면 `.ts` / `.tsx` 로 승격.
- `any` 는 마지막 수단. 불가피하면 왜 필요한지 한 줄 주석.

## 선택 정책

- 상태 관리·라우팅·UI 라이브러리는 MVP 생성기가 산출하는 스펙에 따라 결정되며, 한 MVP 내에서 혼용 금지.
- 생성기 기본 조합(현 시점): Vite + React 18 + TanStack Router + TanStack Query + shadcn/ui(Tailwind + Radix) + react-hook-form + zod.

## Claude 호출 경로

- 프론트엔드에서 Claude API 를 직접 호출하지 않는다 (키 노출 방지 + 중앙 관측).
- 모든 Claude 호출은 **백엔드 경유**.
