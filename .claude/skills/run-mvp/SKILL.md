---
name: run-mvp
description: 이미 생성 + 빌드 검증된 MVP (`generated/<jobId>/`) 를 docker compose 로 띄우고, 호스트에 동적 할당된 포트를 찾아 사용자에게 URL 을 반환.
---

# /run-mvp

## 인자

`/run-mvp <jobId>`

- `jobId` 가 없으면 `generated/` 하위에서 가장 최근 디렉터리 자동 선택, 그 사실을 한 줄로 고지.
- 해당 디렉터리에 `docker-compose.yml` 이 없으면 중단 — "deployer 가 아직 실행되지 않았다. /generate-mvp 재실행 필요" 안내.

## 사전 조건 체크 (시작 직후)

1. `docker info` 가 성공해야 함. 실패하면 "Docker 데몬이 꺼져있음. Docker Desktop / OrbStack 켜라" 안내 후 중단.
2. `generated/<jobId>/artifacts/review.json` 의 `status == "pass"`. pass 가 아니면 중단 — "/review-generated 로 이슈 확인 후 수정".

## 수행 절차

### 1. 빌드 + 기동

```bash
cd generated/<jobId> && docker compose --env-file .env.docker up --build -d
```

- 첫 실행은 backend 이미지 빌드(gradle 의존성 다운로드) 로 3-5 분 소요. stdout 에 진행상황이 보인다고 사용자에게 미리 한 줄 고지.
- 실패 시 `docker compose logs --tail=50` 을 캡처해 보고 후 중단. 부분 기동 상태는 그대로 두고 (디버깅 편의) `docker compose down` 자동 실행 금지.

### 2. 헬스 대기

백엔드 healthy 까지 대기 (deployer 가 healthcheck 를 정의함). 타임아웃 120초.

```bash
for i in $(seq 1 60); do
  state=$(docker compose ps --format json backend | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Health', d.get('State','')))")
  [[ "$state" == "healthy" ]] && break
  sleep 2
done
```

타임아웃이면 `docker compose logs --tail=100 backend` 를 보고 중단.

### 3. 호스트 포트 탐지

```bash
HOST_PORT=$(docker compose port frontend 80 | awk -F: '{print $NF}')
```

빈 값이면 "frontend 포트 매핑 실패 — docker ps 확인" 안내 후 중단.

### 4. 사용자 보고

```
MVP 실행 중 — <jobId>
  URL:      http://localhost:<HOST_PORT>
  admin:    <ADMIN_USERNAME> / (.env.docker 참조)
  로그:     docker compose -f generated/<jobId>/docker-compose.yml logs -f
  종료:     /stop-mvp <jobId>
```

관리자 비밀번호는 **로그에 그대로 찍지 않는다**. `.env.docker` 파일 경로만 알려준다.

## 하지 말 것

- 볼륨 삭제 (`-v` 플래그). 데이터 영속화가 설계 의도.
- `docker system prune` 같은 광범위 cleanup — 다른 docker 작업 영향.
- healthy 대기 없이 포트 노출. 초기 Flyway 마이그레이션 중에는 `/api` 호출이 500 이 난다.
- 이미 같은 jobId 가 띄워져 있으면 `docker compose up` 이 재사용. 충돌이 아니라 재사용이니 그대로 진행하고 보고만 명확히.
