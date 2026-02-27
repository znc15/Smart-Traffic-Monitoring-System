# frontend-vue

Vue3 + TypeScript + ECharts 前端（系统唯一入口）。

## 路由

- `/login`：登录
- `/dashboard`：首页监控
- `/analytics`：分析报表
- `/admin`：管理员

## 本地运行

```bash
cd frontend-vue
pnpm install
pnpm dev --port 5174
```

## 构建

```bash
cd frontend-vue
pnpm build
```

## 接口配置

通过 Vite 环境变量配置后端地址：

- `VITE_API_HTTP_BASE`（默认 `http://localhost:8000`）
- `VITE_API_WS_BASE`（默认 `ws://localhost:8000`）

## 鉴权方式

- `Authorization: Bearer <token>`
- 同时携带 Cookie（`credentials: include`）

## 字段契约

- 读取优先 `snake_case`
- 兼容读取 `camelCase`（过渡期）
