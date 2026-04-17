---
name: stop-mvp
description: 실행 중인 MVP 컨테이너 3종을 내린다. **postgres 볼륨은 유지** — 데이터 보존. 볼륨까지 삭제하려면 `/reset-mvp`.
---

# /stop-mvp

## 인자

`/stop-mvp <jobId>`

- `jobId` 없으면 `/list-mvps` 결과가 1개면 그것을 쓰고, 2개 이상이면 중단하고 목록을 보여줘 사용자에게 선택 요구.
- 해당 jobId 의 `docker-compose.yml` 이 없거나 컨테이너가 이미 내려가 있으면 "이미 정지됨" 보고 후 종료 (에러 아님).

## 수행 절차

```bash
cd generated/<jobId> && docker compose down
```

- `-v` 플래그 쓰지 않는다. 볼륨 유지.
- 종료 후 `docker compose ps` 로 컨테이너가 모두 내려간 것을 확인.

## 보고

```
정지됨 — <jobId>
  볼륨 유지:  agentfactory_<jobId>_pgdata (데이터 보존)
  재실행:    /run-mvp <jobId>
  완전 삭제: /reset-mvp <jobId>
```

## 하지 말 것

- 볼륨 삭제 (`-v`).
- 이미지 삭제 (`--rmi`). 같은 이미지를 다른 job 이 재사용할 수 있다.
- 여러 jobId 를 한 번에 내리기. 항상 1 건씩.
