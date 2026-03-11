#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EDGE_PYTHON="python3"
EDGE_PYTEST="pytest"

if [[ -x "$ROOT_DIR/.venv-edge-test/bin/python" ]]; then
  EDGE_PYTHON="$ROOT_DIR/.venv-edge-test/bin/python"
fi

if [[ -x "$ROOT_DIR/.venv-edge-test/bin/pytest" ]]; then
  EDGE_PYTEST="$ROOT_DIR/.venv-edge-test/bin/pytest"
fi

echo "[gate] backend test"
(
  cd "$ROOT_DIR/backend"
  mvn -B test
)

echo "[gate] edge syntax"
(
  cd "$ROOT_DIR/edge"
  "$EDGE_PYTHON" -m py_compile *.py
)

if [[ "$EDGE_PYTEST" == "pytest" ]] && ! command -v pytest >/dev/null 2>&1; then
  echo "[gate][error] 未找到 pytest，请先执行以下命令准备 Edge 测试环境:"
  echo "  python3 -m venv .venv-edge-test"
  echo "  ./.venv-edge-test/bin/pip install -r edge/requirements-test.txt"
  exit 1
fi

echo "[gate] edge tests"
(
  cd "$ROOT_DIR/edge"
  "$EDGE_PYTEST" -q tests
)

if ! command -v pnpm >/dev/null 2>&1; then
  echo "[gate][error] 未找到 pnpm，请先安装前端依赖工具"
  exit 1
fi

if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "[gate][error] frontend/node_modules 不存在，请先执行: cd frontend && pnpm install"
  exit 1
fi

echo "[gate] frontend test"
(
  cd "$ROOT_DIR/frontend"
  pnpm test
)

echo "[gate] frontend build"
(
  cd "$ROOT_DIR/frontend"
  pnpm build
)

echo "[gate] done"
