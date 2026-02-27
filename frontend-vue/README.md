# frontend-vue（迁移并行版本）

该目录用于执行“React -> Vue + ECharts”并行迁移，当前作为备用前端与现有 `frontend` 并存一个迭代周期。

## 功能覆盖（当前）

- Vue3 + TypeScript + Vite 基础工程
- 页面骨架：登录、首页监控、分析报表、管理员
- ECharts 示例图（流量趋势 + 车型占比）

## 本地运行

```bash
cd frontend-vue
pnpm install
pnpm dev --port 5174
```

默认访问：`http://localhost:5174`

## 生产构建

```bash
cd frontend-vue
pnpm build
pnpm preview --port 4174
```

构建产物目录：`frontend-vue/dist`

## 与 React 前端并行切换建议

1. 保持 `frontend` 作为默认入口，`frontend-vue` 用于灰度验证。
2. 通过网关/Nginx 路由开关切换主入口（例如 `/` -> React，`/vue` -> Vue）。
3. 若灰度阶段出现问题，可立即回切到 React，不影响后端接口契约。

## 契约约束

- 接口字段以 `snake_case` 为标准。
- 迁移期允许读兼容（`camelCase + snake_case`），写入统一按后端契约提交。
