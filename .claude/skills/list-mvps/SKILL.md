---
name: list-mvps
description: `agentfactory.jobId` 라벨이 있는 컨테이너를 조회해 현재 실행 중인 MVP 목록과 URL 을 테이블로 보여준다.
---

# /list-mvps

## 인자

없음.

## 수행 절차

### 1. 컨테이너 목록 조회

```bash
docker ps --filter "label=agentfactory.jobId" \
  --format '{{.Label "agentfactory.jobId"}}\t{{.Names}}\t{{.Ports}}\t{{.Status}}'
```

### 2. jobId 별 그룹핑 + 호스트 포트 추출

한 jobId 당 3 컨테이너(mysql, backend, frontend) 가 나오므로 `frontend` 서비스의 `Ports` 필드에서 호스트 포트만 추출. `0.0.0.0:xxxxx->80/tcp` 패턴.

### 3. 디스크 상 jobId 와 교차 검증

```bash
ls generated/ 2>/dev/null
```

디스크에 있지만 실행 중 아닌 job 도 구분해서 보여준다 (시연 자료로 유용).

## 출력 형식

```
실행 중:
  <jobId>    http://localhost:<port>    healthy 2h ago
  <jobId2>   http://localhost:<port>    healthy 10m ago

정지 상태 (디스크에만 존재):
  <jobId3>   generated/<jobId3>/        (마지막 수정: 3일 전)

전체: 실행 2, 정지 1
```

컨테이너 0 개 + 디스크 0 개면 "현재 MVP 없음. /generate-mvp 로 시작." 안내.

## 하지 말 것

- docker 데몬이 꺼져있을 때 crash. `docker info` 실패하면 "Docker 꺼져있음, 디스크 목록만 표시" 로 fallback.
- 라벨 없는 컨테이너까지 포함 (사용자의 다른 docker 작업과 혼동 금지).
- 비밀값(`ADMIN_PASSWORD` 등) 를 출력에 노출.
