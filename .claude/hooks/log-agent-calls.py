#!/usr/bin/env python3
"""PostToolUse on Agent — 서브에이전트 호출 1건당 JSON 한 줄을 `.claude/logs/agent-calls.log` 에 append.

MVP 생성 파이프라인 디버깅(프롬프트 drift, 응답 크기, 재시도 패턴 파악)을 위한 개발용 관측.
런타임 Kotlin 백엔드는 Claude API 를 직접 호출하므로 이 훅 범위 밖 — 프로덕션 관측은 별도.
"""
import json, os, sys
from datetime import datetime


data = json.load(sys.stdin)
ti = data.get("tool_input", {})
tr = data.get("tool_response", {})

subagent = ti.get("subagent_type", "general-purpose")
prompt = ti.get("prompt", "")
desc = ti.get("description", "")

if isinstance(tr, dict):
    result = tr.get("content") or tr.get("output") or ""
    if isinstance(result, list):
        result = " ".join(str(x) for x in result)
else:
    result = str(tr)

cwd = data.get("cwd", os.getcwd())
log_dir = os.path.join(cwd, ".claude", "logs")
os.makedirs(log_dir, exist_ok=True)

entry = {
    "ts": datetime.now().isoformat(timespec="seconds"),
    "session": data.get("session_id", ""),
    "subagent": subagent,
    "description": desc,
    "prompt_chars": len(prompt) if isinstance(prompt, str) else 0,
    "result_chars": len(result) if isinstance(result, str) else 0,
}

with open(os.path.join(log_dir, "agent-calls.log"), "a", encoding="utf-8") as f:
    f.write(json.dumps(entry, ensure_ascii=False) + "\n")

sys.exit(0)
