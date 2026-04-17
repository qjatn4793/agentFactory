---
name: reset-mvp
description: MVP 컨테이너 + **postgres 볼륨까지** 완전 삭제. 초기 상태로 되돌릴 때만 사용. 파괴적 작업이므로 반드시 사용자 확인 후 실행.
---

# /reset-mvp

## 인자

`/reset-mvp <jobId>`

- `jobId` 생략 금지 (파괴적 작업이므로 명시성 요구).

## 사전 확인 — 파괴적 경고 필수

실행 전에 사용자에게 다음 메시지를 **먼저 출력하고 명시적 확인을 받는다**:

```
경고: 다음 항목이 영구 삭제됩니다.
  - 컨테이너: postgres, backend, frontend (jobId=<jobId>)
  - 볼륨: agentfactory_<jobId>_pgdata (DB 데이터 전체)
  - 빌드된 이미지는 유지.
  - 소스 코드 파일은 유지.

계속하려면 "yes, reset <jobId>" 라고 답해주세요.
```

사용자가 정확히 위 문구로 응답하지 않으면 중단. 대충 "yes" 만 쳐도 거절 (의도 확인 장치).

## 수행 절차 (확인 통과 후)

```bash
cd generated/<jobId> && docker compose down -v
```

`-v` 로 명명 볼륨(`agentfactory_<jobId>_pgdata`) 까지 제거.

## 보고

```
초기화 완료 — <jobId>
  컨테이너: 삭제됨
  볼륨:    agentfactory_<jobId>_pgdata 삭제됨
  이미지:  유지 (다음 /run-mvp 시 재사용)
  다시 실행: /run-mvp <jobId>  (Flyway 가 빈 DB 에 V001 부터 재실행)
```

## 하지 말 것

- 사용자 확인 없이 실행 (CLAUDE.md 파괴적 작업 규칙).
- 이미지까지 삭제 (`--rmi`). 재빌드 시간 크게 증가.
- `docker volume prune` 같은 광범위 명령.
- jobId 생략 허용 (최신 자동 선택). 실수로 잘못된 job 의 DB 를 날릴 수 있다.
