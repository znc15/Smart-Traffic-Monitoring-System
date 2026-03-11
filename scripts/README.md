# 脚本工具集

仓库根目录下的自动化脚本，覆盖本地门禁、环境清理、数据库灰度和性能采样。

## 使用约定

- 所有脚本都从仓库根目录执行
- 运行环境以 macOS / Linux + Bash 为主
- 涉及数据库切换和一致性检查的脚本依赖根 `docker-compose.yml`

## 脚本清单

| 脚本 | 功能 |
|------|------|
| `./scripts/local-gate.sh` | 本地门禁 |
| `./scripts/clean-project.sh` | 清理依赖与构建缓存 |
| `bash scripts/check_mirror_consistency.sh` | PostgreSQL / MySQL 一致性检查 |
| `./scripts/db/switch_primary.sh <postgres|mysql>` | 数据库主库切换 |
| `./scripts/perf/run_perf_bundle.sh` | 性能证据采样 |

## local-gate.sh

```bash
./scripts/local-gate.sh
```

执行顺序：
1. `backend`：`mvn -B test`
2. `edge`：`python -m py_compile *.py` + `pytest -q tests`
3. `frontend`：`pnpm test` + `pnpm build`

注意：
- 如果系统没有全局 `pytest`，脚本会提示你准备 `.venv-edge-test`
- `frontend/node_modules` 必须已存在，否则脚本会直接失败

准备 Edge 测试环境：

```bash
python3 -m venv .venv-edge-test
./.venv-edge-test/bin/pip install -r edge/requirements-test.txt
```

## check_mirror_consistency.sh

默认检查：
- `traffic_samples`
- `traffic_events`
- `traffic_predictions`

用法：

```bash
bash scripts/check_mirror_consistency.sh
bash scripts/check_mirror_consistency.sh --since 2026-03-11T00:00:00
bash scripts/check_mirror_consistency.sh --all
```

说明：
- `--since` 只对带 `created_at` 的流量相关表做增量检查
- `--all` 会扩展到 `users`、`cameras`、`site_settings`、`api_clients` 等表

## db/switch_primary.sh

```bash
./scripts/db/switch_primary.sh postgres
./scripts/db/switch_primary.sh mysql
```

行为说明：
- 脚本不会直接改配置文件
- 它会通过临时环境变量重建 `backend` 容器
- 切换后会等待 `backend` 进入 `healthy`

推荐流程：

```bash
bash scripts/check_mirror_consistency.sh
./scripts/db/switch_primary.sh postgres
bash scripts/check_mirror_consistency.sh --since 2026-03-11T00:00:00
```

## perf/run_perf_bundle.sh

```bash
./scripts/perf/run_perf_bundle.sh
```

默认行为：
- 采样 backend 预测接口延迟
- 如可访问 edge，则额外采样 edge 指标
- 结果输出到 `docs/reports/` 与 `docs/reports/raw/`

可用环境变量：

| 变量 | 说明 |
|------|------|
| `EDGE_URL` | edge 根地址，默认 `http://localhost:9000` |
| `EDGE_CONFIG_URL` | edge 配置接口地址 |
| `BACKEND_URL` | backend 根地址 |
| `ROAD_NAME` | 测试道路名称 |
| `SAMPLES` | 采样次数 |
| `SKIP_EDGE` | 是否跳过 edge |
| `EDGE_NODE_ID` | edge 节点 ID |
| `EDGE_KEY` | edge 鉴权 key |
| `COLLECT_OPENVINO_COMPARE` | 是否采集 OpenVINO 对比数据 |

如果你的 edge 按默认端口 `8000` 启动，请显式覆盖：

```bash
EDGE_URL=http://localhost:8000 ./scripts/perf/run_perf_bundle.sh
```

## clean-project.sh

```bash
./scripts/clean-project.sh
```

用途：
- 清理 `node_modules`
- 清理 `target`、`dist`、`__pycache__`
- 在依赖异常或切分支后恢复干净状态

## 注意事项

1. 数据库切换前先跑一致性检查，避免带着脏数据切库。
2. `perf/run_perf_bundle.sh` 会产生明显负载，不建议直接在生产环境执行。
3. `clean-project.sh` 会删除依赖目录，执行后通常需要重新安装依赖。
