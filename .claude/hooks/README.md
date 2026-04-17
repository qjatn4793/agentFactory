# hooks/

Claude Code 훅 스크립트. `.claude/settings.json` 의 `hooks` 섹션에서 배선됨.

## 현재 훅

| 파일 | 이벤트 | 매처 | 역할 |
|---|---|---|---|
| [stack-enforce.py](stack-enforce.py) | `PreToolUse` | `Write\|Edit` | frontend에 `.jsx` / `@anthropic-ai/sdk` 차단, backend에 `.java` 차단 |
| [format-on-write.py](format-on-write.py) | `PostToolUse` | `Write\|Edit` | 편집 파일 자동 포맷(prettier/ktlint). 포매터 없으면 조용히 스킵 |
| [log-agent-calls.py](log-agent-calls.py) | `PostToolUse` | `Agent` | 서브에이전트 호출을 `.claude/logs/agent-calls.log` 에 JSONL로 append |

모든 훅은 **Python 3** (macOS/Linux 기본). 외부 의존(jq 등) 없음.

## 동작 규약

- **stack-enforce**: `block: <reason>` 을 stderr로 출력 + exit 2 → 해당 Write/Edit 차단, 이유를 Claude에게 피드백.
- **format-on-write / log-agent-calls**: 항상 exit 0. 실패해도 작업을 막지 않는다 (관측·편의 목적).
- 경로 판정은 path의 세그먼트에 `frontend` / `backend` 가 포함되는지로 결정. `generated/<jobId>/frontend/**` 도 자동 적용.

## 스택 강제 범위 (stack-enforce)

| 경로 | 차단 | 이유 |
|---|---|---|
| `**/frontend/**/*.jsx` | 파일 생성 차단 | TypeScript 강제 |
| `**/frontend/**` 내용에 `@anthropic-ai/sdk` import | 파일 쓰기 차단 | Claude 호출은 백엔드에서만 |
| `**/backend/**/*.java` | 파일 생성 차단 | Kotlin 강제 |

의도적인 예외는 현재 없음. 필요해지면 화이트리스트 섹션을 스크립트 상단에 추가.

## 로그 (log-agent-calls)

- 경로: `.claude/logs/agent-calls.log` (JSON Lines).
- 스키마: `{ ts, session, subagent, description, prompt_chars, result_chars }`.
- **개발 관측 전용** — 실제 agentFactory 런타임(Kotlin 백엔드)은 Claude API 를 직접 호출하므로 이 로그 범위 밖. 런타임 관측은 별도 설계 필요.

## 새 훅 추가 절차

1. `.claude/hooks/<name>.py` 작성 (`#!/usr/bin/env python3`, `chmod +x`).
2. 표준 입력에서 JSON 파싱: `data = json.load(sys.stdin)`. 주요 키: `tool_name`, `tool_input`, `tool_response`, `cwd`, `session_id`.
3. `.claude/settings.json` 의 `hooks` 섹션에 항목 추가. 명령은 `$CLAUDE_PROJECT_DIR/.claude/hooks/<name>.py` 형태로.
4. 차단이 목적이면 `stderr` 출력 + exit 2. 그 외는 exit 0 유지.
