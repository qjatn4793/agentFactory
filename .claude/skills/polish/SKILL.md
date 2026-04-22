---
name: polish
description: 기존 MVP(`generated/<jobId>/`) 의 완성도 갭을 진단(polish-auditor) → 사용자 선택 → 적용(polish-applier) → qa 재검증. 기능 추가가 아닌 UX · 견고성 · 중복 제거 중심. 신규 엔티티는 `/add-entity`, 기존 엔티티 필드 확장은 `/modify-entity` 사용.
---

# /polish

## 인자

`/polish <jobId> [itemIds...]`

- `jobId` 생략 시 `generated/` 의 가장 최근 디렉터리 자동 선택 (고지).
- `itemIds` 는 스페이스 구분 (예: `P1-UX-001 P1-RB-003`). 생략 시 **진단 리포트만** 보여주고 적용 전 사용자 확인을 받는다.
- 특수 키워드:
  - `all` — 리포트의 모든 항목 적용 (위험 — 확인 필수).
  - `p1` — P1 항목 전부.

## 사전 조건

1. `generated/<jobId>/` 존재.
2. `artifacts/review.json` 이 `status=pass`. fail 이면 중단 — "/review-generated 로 계약 이슈 먼저 해결".
3. MVP 가 현재 띄워져 있을 필요는 없음 (감사는 정적). 단, 적용 후 변경을 확인하려면 `/stop-mvp` → `/run-mvp` 로 재기동.

## 수행 절차

### 1. polish-auditor 호출

- `Agent(subagent_type: "polish-auditor", prompt: "<workspaceDir 절대경로 + 아티팩트 경로>")`.
- 반환 `PolishAuditReport` 를 `artifacts/polish-audit.json` 에 저장 (덮어쓰기 시 `polish-audit.<timestamp>.json` 백업).

### 2. 사용자에게 진단 결과 표시

```
polish audit: <jobId>
요약: <summary>

[P1]
  P1-UX-001  ContactListPage empty state 없음
             scope: src/pages/contacts/ContactListPage.tsx
             effort: small
  P1-RB-003  삭제 후 낙관적 업데이트 없어 UI 깜빡임
             scope: src/pages/*/List*.tsx (3 files)
             effort: medium
[P2]
  ...

적용하려면: /polish <jobId> P1-UX-001 P1-RB-003
또는:       /polish <jobId> p1   (P1 전체)
```

`itemIds` 가 처음부터 주어졌으면 이 리스트는 **요약만** 출력하고 3번으로 진행.

### 3. polish-applier 호출 (itemIds 지정된 경우)

- 키워드 해석: `all` → 전체, `p1` → priority=P1 인 항목만, 그 외는 명시 id 그대로.
- 존재하지 않는 id 는 `skipped` 에 기록되고 중단하지 않는다 (사용자 오타 허용).
- `Agent(subagent_type: "polish-applier", prompt: "<workspaceDir + audit + selectedIds>")`.
- 반환 `PolishApplyReport` 를 `artifacts/polish-apply.<timestamp>.json` 에 저장.

### 4. qa-reviewer 재실행 확인

polish-applier 가 내부에서 이미 호출하지만, 이 스킬 레벨에서도 `artifacts/review.json` 의 최종 상태가 `pass` 인지 다시 확인. `fail` 이면 사용자에게 경고 (applier 가 revert 못 한 경우 대비).

### 5. 사용자 보고

```
polish 완료 — <jobId>
적용됨: N건
  [P1-UX-001] ContactListPage empty state 추가 — 1 file
  [P1-RB-003] 낙관적 업데이트 도입 — 3 files
건너뜀: M건
  [P2-CD-004] (audit 에 없음)
  [P1-FG-002] (회귀 발생으로 revert)
qa: pass / issues=0

다음: /stop-mvp <jobId> && /run-mvp <jobId>   (변경 반영)
```

## 권장 사용 패턴

1. 먼저 `/polish <jobId>` (id 없이) 로 리포트만 본다.
2. P1 우선 훑고 의문 나는 항목은 건너뛴다.
3. `/polish <jobId> p1` 으로 P1 일괄 적용 또는 원하는 id 만 선택.
4. 런타임 재기동 후 체감 확인.
5. 여유 있으면 P2 항목 선별.

## 하지 말 것

- 스킬 안에서 자동으로 `all` 적용. 파괴 범위가 크므로 사용자가 명시해야 한다.
- 새 엔티티/스키마 변경을 폴리싱 항목으로 섞어 받기 — auditor 가 이미 범위 밖으로 거르지만, 이 스킬도 한 번 더 확인.
- qa fail 상태로 보고 종료. applier 가 복원 못 하면 사용자에게 수동 조치 지시.
- 컨테이너 자동 재기동. 파일 변경만 하고 `/run-mvp` 는 사용자 결정.
