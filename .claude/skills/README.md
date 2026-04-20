# skills/

프로젝트 전용 스킬(사용자 호출형 `/스킬이름`). Claude Code가 이 디렉터리의 하위 폴더를 자동으로 로드한다.

## 구조

각 스킬은 **하위 디렉터리 + `SKILL.md`** 형태.

```
skills/
  generate-mvp/
    SKILL.md        # 스킬 정의 (frontmatter + 지침)
    template.md     # 선택: 참고 템플릿
  review-generated/
    SKILL.md
```

`SKILL.md` frontmatter 예시:

```markdown
---
name: generate-mvp
description: 한 줄 아이디어를 받아 전체 MVP 생성 파이프라인(planner → schema → backend → frontend → qa)을 실행한다.
---

<사용법, 인자, 기대 결과>
```

## 현재 스킬

### 생성 파이프라인
- [generate-mvp/SKILL.md](generate-mvp/SKILL.md) — 한 줄 아이디어 → 전체 파이프라인(planner→...→deployer) 실행
- [review-generated/SKILL.md](review-generated/SKILL.md) — 기존 MVP 에 대해 qa-reviewer 재실행, 리포트만
- [add-entity/SKILL.md](add-entity/SKILL.md) — 기존 MVP 에 엔티티 1개 **증분 추가** (기존 파일 비파괴)
- [polish/SKILL.md](polish/SKILL.md) — 완성도 갭 진단(polish-auditor) → 선택 → 적용(polish-applier) → qa 재검증. UX · 견고성 · 중복 제거 중심

### 런타임 (docker compose)
- [run-mvp/SKILL.md](run-mvp/SKILL.md) — `docker compose up --build -d` + 호스트 포트 탐지 + URL 출력
- [stop-mvp/SKILL.md](stop-mvp/SKILL.md) — 컨테이너 내림, 볼륨 유지
- [list-mvps/SKILL.md](list-mvps/SKILL.md) — 실행 중 MVP 목록 + URL 테이블
- [reset-mvp/SKILL.md](reset-mvp/SKILL.md) — 컨테이너 + mysql 볼륨 완전 삭제 (파괴적 · 사용자 확인 필수)

## 아티팩트 레이아웃 (스킬 간 공유 계약)

```
generated/<jobId>/
  artifacts/
    spec.json        # mvp-planner
    schema.json      # schema-designer
    backend.json     # backend-builder 요약
    frontend.json    # frontend-builder 요약
    review.json      # qa-reviewer (덮어쓰기 시 .<timestamp>.json 백업)
    deploy.json      # deployer (파일 목록 + 서비스명 + 라벨)
  backend/           # backend-builder + deployer(Dockerfile)
  frontend/          # frontend-builder + deployer(Dockerfile, nginx.conf)
  docker-compose.yml # deployer
  .env.docker        # deployer (비밀값 포함 — gitignore 대상)
```

스킬은 이 레이아웃을 **단일 규약**으로 공유한다. 런타임(Kotlin 백엔드)도 동일 레이아웃으로 작업공간을 구성한다.
