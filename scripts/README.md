# 脚本工具集

> 智能交通监控系统的自动化脚本，提供本地门禁、项目清理、数据库管理和性能测试等能力。

---

## 脚本清单

| 脚本 | 路径 | 功能 |
|------|------|------|
| 本地门禁 | `local-gate.sh` | 构建 + 测试 + 联调一键检查 |
| 项目清理 | `clean-project.sh` | 清理依赖与构建缓存 |
| 镜像一致性检查 | `check_mirror_consistency.sh` | PostgreSQL ↔ MySQL 数据一致性校验 |
| 数据库主从切换 | `db/switch_primary.sh` | 在 PostgreSQL / MySQL 之间切换主库 |
| 性能测试套件 | `perf/run_perf_bundle.sh` | 执行完整性能测试并生成报告 |

---

## 前置条件

- **操作系统**：macOS / Linux（脚本使用 Bash 编写）
- **Node.js**：项目构建与测试依赖
- **Docker**：部分脚本需要 Docker 环境运行数据库容器
- **数据库**：PostgreSQL 和/或 MySQL 实例（视使用的脚本而定）
- **权限**：脚本需要可执行权限，首次使用请运行 `chmod +x scripts/*.sh scripts/**/*.sh`

---

## 详细用法

### 1. local-gate.sh — 本地门禁

一键执行后端测试、Edge 测试、前端测试与前端构建，确保提交前代码质量达标。

```bash
./scripts/local-gate.sh
```

适用于：
- 提交代码前的自检
- CI 本地预演，提前发现问题

当前门禁顺序：
1. `backend` — `mvn -B test`
2. `edge` — `python3 -m py_compile *.py` + `pytest -q tests`
3. `frontend` — `pnpm test` + `pnpm build`

---

### 2. clean-project.sh — 项目清理

清理 `node_modules`、`target`、`__pycache__`、`dist` 等构建产物与缓存目录，释放磁盘空间并确保干净的构建环境。

```bash
./scripts/clean-project.sh
```

适用于：
- 依赖异常时重置环境
- 切换分支后清理残留产物

---

### 3. check_mirror_consistency.sh — 数据库镜像一致性检查

校验 PostgreSQL 与 MySQL 双数据库镜像间的数据一致性，输出差异报告。

```bash
bash scripts/check_mirror_consistency.sh
```

适用于：
- 数据库同步后的验证
- 主从切换前的安全检查

---

### 4. db/switch_primary.sh — 数据库主从切换

在 PostgreSQL 和 MySQL 之间切换主数据库，自动更新应用配置。

```bash
# 切换到 PostgreSQL 作为主库
./scripts/db/switch_primary.sh postgres

# 切换到 MySQL 作为主库
./scripts/db/switch_primary.sh mysql
```

适用于：
- 数据库故障时的快速切换
- 对比不同数据库引擎的性能表现

---

### 5. perf/run_perf_bundle.sh — 性能测试套件

执行完整的性能测试流程，涵盖接口响应时间、并发吞吐量等指标。

```bash
./scripts/perf/run_perf_bundle.sh
```

适用于：
- 版本发布前的性能验收
- 生成答辩所需的性能证据数据

---

## 注意事项

1. **执行目录**：所有脚本应从项目根目录执行，不要 `cd` 到 `scripts/` 目录后运行。
2. **环境变量**：部分脚本依赖 `.env` 文件中的数据库连接信息，请确保已正确配置。
3. **数据安全**：`switch_primary.sh` 会修改数据库配置，切换前请先运行 `check_mirror_consistency.sh` 确认数据一致。
4. **性能测试**：`run_perf_bundle.sh` 会产生较大负载，避免在生产环境直接执行。
5. **清理风险**：`clean-project.sh` 会删除依赖目录，执行后需重新安装依赖（`npm install` 等）。
