# mvp-generation rules

"아이디어 → MVP" 파이프라인을 구성·수정할 때의 지침.

## 파이프라인 단계

1. **planner**: 한 줄 아이디어 → MVP 스펙 (엔티티, 관계, 핵심 화면, 핵심 API).
2. **schema-designer**: 스펙 → DB 스키마 + 초기 마이그레이션.
3. **backend-builder**: 스펙 + 스키마 → Kotlin API (CRUD, 인증 스텁, OpenAPI).
4. **frontend-builder**: 스펙 + OpenAPI → TypeScript admin UI (리스트/상세/폼 뷰).
5. **qa-reviewer**: 산출물 일관성 검증 (스펙 ↔ 스키마 ↔ API ↔ UI).
6. **deployer**: backend/frontend 에 Dockerfile, nginx.conf, 루트에 `docker-compose.yml` + `.env.docker` 생성. **qa pass 필수**.

각 단계는 **명시적 입력/출력 계약**을 가진다. 다음 단계는 이전 단계의 구조화된 산출물(JSON/마크다운)만 신뢰한다 — 자유 텍스트를 다시 파싱하지 않는다.

## 기본 MVP 형상 (admin 지향)

- 엔티티당 기본 제공: 리스트 · 상세 · 생성/수정 폼 · 삭제.
- 검색/필터/페이지네이션은 리스트 뷰에 포함.
- 인증: 초기에는 단일 admin 계정 스텁. 세션은 백엔드가 관리.
- 관측: 모든 생성 작업은 `jobId`로 추적 가능해야 하며, 단계별 상태/로그를 남긴다.

## 하지 말 것

- planner 단계에서 구현 세부(라이브러리 버전, 파일 경로)까지 결정하지 않는다 — builder 단계의 역할.
- 하나의 큰 프롬프트로 전 단계를 생성하려 하지 않는다. 단계 분리가 디버깅·재시도 가능성의 핵심.
- 생성물 안에 "TODO 나중에 채울 것" 같은 반쪽짜리 스텁을 남기지 않는다. 범위에 없으면 생성하지 않는다.
