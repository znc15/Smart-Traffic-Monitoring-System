#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[clean] project root: ${ROOT_DIR}"

echo "[clean] removing local caches and build outputs"
export ROOT_DIR
python3 - <<'PY'
import os
import shutil
from pathlib import Path

root = Path(os.environ["ROOT_DIR"])

targets = [
    root / "frontend-vue" / "node_modules",
    root / "frontend-vue" / "dist",
    root / "backend" / "target",
    root / ".venv",
    root / ".pytest_cache",
    root / "edge" / ".pytest_cache",
]

for path in targets:
    if path.exists():
        shutil.rmtree(path)
        print(f"removed: {path.relative_to(root)}")

for pycache in root.rglob("__pycache__"):
    if pycache.is_dir():
        shutil.rmtree(pycache)
        print(f"removed: {pycache.relative_to(root)}")
PY

echo "[clean] done"
