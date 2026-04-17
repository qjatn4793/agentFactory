#!/usr/bin/env python3
"""PostToolUse on Write|Edit — 편집된 파일을 포맷. 포매터 없으면 조용히 스킵."""
import json, os, shutil, subprocess, sys


data = json.load(sys.stdin)
path = data.get("tool_input", {}).get("file_path", "")

if not path or not os.path.isfile(path):
    sys.exit(0)

ext = os.path.splitext(path)[1].lower()
ts_like = {".ts", ".tsx", ".js", ".jsx", ".json", ".md", ".yml", ".yaml", ".css"}
kt_like = {".kt", ".kts"}


def nearest_with(filename: str, start: str) -> str | None:
    d = os.path.dirname(os.path.abspath(start))
    while d and d != "/":
        if os.path.isfile(os.path.join(d, filename)):
            return d
        d = os.path.dirname(d)
    return None


try:
    if ext in ts_like and shutil.which("npx"):
        proj = nearest_with("package.json", path)
        if proj:
            subprocess.run(
                ["npx", "--no-install", "prettier", "--write", path],
                cwd=proj, capture_output=True, timeout=20, check=False,
            )
    elif ext in kt_like and shutil.which("ktlint"):
        subprocess.run(
            ["ktlint", "-F", path],
            capture_output=True, timeout=20, check=False,
        )
except Exception:
    pass

sys.exit(0)
