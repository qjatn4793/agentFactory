# frontend/stack

agentFactory 프론트엔드 스택 규칙. 회귀시키지 않는다.

## 언어

- **TypeScript** 고정. `.js` 로 회귀 금지. 필요하면 `.ts` / `.tsx` 로 승격.
- `any` 는 마지막 수단. 불가피하면 왜 필요한지 한 줄 주석.

## 선택 정책

- 상태 관리·라우팅·UI 라이브러리는 MVP 생성기가 산출하는 스펙에 따라 결정되며, 한 MVP 내에서 혼용 금지.
- 생성기 기본 조합(현 시점): Vite + React 18 + TanStack Router + TanStack Query + shadcn/ui(Tailwind + Radix) + react-hook-form + zod.

## TanStack Router 규칙

### validateSearch + navigate 조합

- 라우트에 `validateSearch` 를 선언하면 그 라우트로의 `navigate({ to })` 호출 시 **`search` 인자가 required 로 추론**된다. 생략하면 TS 빌드 실패 (`Property 'search' is missing in type '{ to: ... }'`).
- 특히 `/login` 라우트에 `redirect` 같은 선택 search 파라미터를 받으려고 `validateSearch` 를 추가한 경우, 다른 곳에서 `navigate({ to: "/login" })` 하면 TS 에러.
- **해결**: `navigate({ to: "/login", search: { redirect: undefined } })` 처럼 빈 객체 또는 undefined 값을 명시. `<Link to="/login" search={{ redirect: undefined }}>` 도 마찬가지.
- 자주 틀리는 곳: 로그아웃 핸들러, 401 인터셉터, protected 라우트의 beforeLoad redirect.

### Route id 와 useParams

- 부모 라우트가 pathless layout 용으로 `id: "authed"` 같은 id 만 가지면, 자식 라우트의 실제 route id 는 `/authed/<child-path>` 형태로 중첩된다.
- 자식 페이지에서 `useParams({ from: "/child-path" })` 로 쓰면 미스매치 에러. 반드시 **실제 중첩된 전체 경로** (`/authed/child-path`) 또는 export 된 route 객체의 `.useParams()` 사용.

## Claude 호출 경로

- 프론트엔드에서 Claude API 를 직접 호출하지 않는다 (키 노출 방지 + 중앙 관측).
- 모든 Claude 호출은 **백엔드 경유**.
