#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[gate] frontend lint"
(
  cd "$ROOT_DIR/frontend"
  pnpm lint
)

echo "[gate] frontend build"
(
  cd "$ROOT_DIR/frontend"
  pnpm build
)

echo "[gate] backend test"
(
  cd "$ROOT_DIR/backend"
  mvn -B test
)

echo "[gate] edge syntax"
(
  cd "$ROOT_DIR/edge"
  python3 -m py_compile *.py
)

echo "[gate] edge tests"
(
  cd "$ROOT_DIR/edge"
  pytest -q tests
)

echo "[gate] docker compose up"
(
  cd "$ROOT_DIR"
  docker compose up --build -d
)

echo "[gate] done"
