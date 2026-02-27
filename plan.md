# 项目瘦身计划（执行状态版）

更新时间：2026-02-27

## 目标

在不改变业务功能的前提下完成项目减负：

1. 前端改为 Vue 单栈。
2. 删除原始证据与临时实测文件。
3. 清理本地依赖与构建缓存。
4. 同步修复运行链路与文档引用。

## 执行项

### M1：前端链路精简

- [x] 下线 `frontend-react` 容器
- [x] 网关移除 `/react` 代理并返回 404
- [x] 本地门禁移除 React 构建
- [x] CI 移除 React job
- [x] 删除 `frontend` 目录

### M2：证据与文档清理

- [x] 删除 `docs/defense/screenshots`
- [x] 删除 `docs/defense/evidence`
- [x] 删除 `docs/reports/raw`
- [x] 删除 `docs/reports/closure-validation-2026-02-27.md`
- [x] 删除 `docs/reports/perf-summary-20260227_111107.md`
- [x] 清理 README/部署文档/答辩文档中的失效引用

### M3：本地缓存清理

- [x] 删除 `frontend-vue/node_modules`
- [x] 删除 `.venv`
- [x] 删除 `frontend-vue/dist`
- [x] 删除 `backend/target`
- [x] 删除 `.pytest_cache` 与 `edge/.pytest_cache`
- [x] 删除全仓 `__pycache__`
- [x] 新增 `scripts/clean-project.sh`

## 验收标准

- `rg -n "/react|frontend-react|working-directory: frontend|docs/defense/screenshots|docs/reports/raw|closure-validation"` 仅保留有效项（`/react` 404 校验与 `@vue/reactivity` 误匹配）。
- `pnpm -C frontend-vue build` 通过。
- `mvn -B -f backend/pom.xml test` 通过。
- `python3 -m py_compile edge/*.py && pytest -q edge/tests` 通过。
- `docker compose up --build -d` 成功，关键服务 healthy。
