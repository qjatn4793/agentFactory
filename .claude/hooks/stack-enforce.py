#!/usr/bin/env python3
"""PreToolUse on Write|Edit — TS/Kotlin-only 스택 규칙을 강제."""
import json, os, re, sys


def block(reason: str) -> None:
    print(f"BLOCKED: {reason}", file=sys.stderr)
    sys.exit(2)


data = json.load(sys.stdin)
ti = data.get("tool_input", {})
path = ti.get("file_path", "")
content = ti.get("new_string") or ti.get("content") or ""

if not path:
    sys.exit(0)

cwd = data.get("cwd", os.getcwd())
rel = os.path.relpath(path, cwd) if os.path.isabs(path) else path
parts = set(rel.split(os.sep))

is_frontend = "frontend" in parts
is_backend = "backend" in parts

if is_frontend:
    if rel.endswith(".jsx"):
        block("frontend는 .jsx 대신 .tsx 사용 (stack rule). 파일명을 .tsx로 변경.")
    if re.search(r"@anthropic-ai/sdk|from\s+['\"]anthropic['\"]|require\(['\"]anthropic", content):
        block("프론트엔드에서 Claude SDK를 import하지 않는다 (stack rule). 백엔드 API를 경유할 것.")

if is_backend:
    if rel.endswith(".java"):
        block("backend는 Kotlin만 사용 (stack rule). 파일명을 .kt로 변경.")

sys.exit(0)
