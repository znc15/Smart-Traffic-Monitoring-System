# Smart Traffic Monitoring — Frontend

> 智能交通监控系统前端，提供实时监控大屏、数据分析报表与系统管理界面。

![Vue](https://img.shields.io/badge/Vue-3.5.13-4FC08D?logo=vuedotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-5.4-646CFF?logo=vite&logoColor=white)
![Naive UI](https://img.shields.io/badge/Naive_UI-2.43-18A058?logo=vue.js&logoColor=white)
![ECharts](https://img.shields.io/badge/ECharts-5.5-AA344D?logo=apacheecharts&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 目录

- [技术栈](#技术栈)
- [页面路由](#页面路由)
- [目录结构](#目录结构)
- [本地开发](#本地开发)
- [生产构建](#生产构建)
- [接口配置](#接口配置)
- [鉴权方式](#鉴权方式)
- [字段契约](#字段契约)

---

## 技术栈

| 类别 | 技术 | 版本 |
|:-----|:-----|:-----|
| 框架 | Vue 3 (Composition API) | 3.5.13 |
| 语言 | TypeScript | 5.7 |
| 构建 | Vite | 5.4 |
| UI 组件库 | Naive UI | 2.43 |
| 可视化 | ECharts | 5.5 |
| 路由 | Vue Router | 4.5 |
| 自动导入 | unplugin-auto-import / unplugin-vue-components | — |
| 部署 | Nginx + Docker | — |

---

## 页面路由

| 路径 | 视图组件 | 说明 |
|:-----|:---------|:-----|
| `/login` | `LoginView.vue` | 用户登录 / 注册页 |
| `/dashboard` | `DashboardView.vue` | 首页监控大屏 — 实时路况、车流统计、告警信息 |
| `/map` | `MapView.vue` | GIS 地图 — 高德底图、拥堵热力、节点快照 |
| `/analytics` | `AnalyticsView.vue` | 分析报表 — 历史趋势、多维度对比 |
| `/admin` | `AdminView.vue` | 管理后台 — 用户管理、Edge 节点管理、系统配置 |

---

## 目录结构

```
frontend/
├── src/
│   ├── components/          # 公共组件（AppLayout 等）
│   ├── views/               # 页面视图
│   │   ├── LoginView.vue
│   │   ├── DashboardView.vue
│   │   ├── MapView.vue
│   │   ├── AnalyticsView.vue
│   │   └── AdminView.vue
│   ├── lib/                 # 工具库
│   │   ├── api.ts           #   API 请求封装
│   │   ├── amap.ts          #   高德地图脚本加载
│   │   └── normalize.ts     #   数据格式化 / 字段兼容
│   ├── store/               # 状态管理
│   ├── App.vue              # 根组件
│   ├── main.ts              # 应用入口
│   ├── router.ts            # 路由配置
│   └── theme.ts             # Naive UI 主题定制
├── package.json
├── vite.config.ts           # Vite 配置（代理、插件）
├── tsconfig.json
├── nginx.conf               # 生产环境 Nginx 配置
├── Dockerfile
└── README.md
```

---

## 本地开发

**前置条件**：Node.js ≥ 18、pnpm ≥ 8

```bash
cd frontend
pnpm install
pnpm dev --port 5174
```

启动后访问 [http://localhost:5174](http://localhost:5174)。

---

## 生产构建

```bash
cd frontend
pnpm build
```

产物输出至 `dist/`，可直接挂载到 Nginx 或通过 Docker 镜像部署：

```bash
docker build -t traffic-frontend .
docker run -p 80:80 traffic-frontend
```

---

## 接口配置

通过 Vite 环境变量配置后端地址，可在项目根目录创建 `.env.local` 覆盖默认值：

| 变量 | 说明 | 默认值 |
|:-----|:-----|:-------|
| `VITE_API_HTTP_BASE` | 后端 HTTP 接口基址 | `http://localhost:8000` |
| `VITE_API_WS_BASE` | 后端 WebSocket 基址 | `ws://localhost:8000` |
| `VITE_AMAP_KEY` | 高德地图 Web JS API Key | 空 |

示例 `.env.local`：

```env
VITE_API_HTTP_BASE=https://api.example.com
VITE_API_WS_BASE=wss://api.example.com
VITE_AMAP_KEY=your_amap_web_js_key
```

---

## 鉴权方式

所有 API 请求同时携带两种凭证，确保兼容不同部署场景：

1. **Bearer Token** — 请求头 `Authorization: Bearer <token>`
2. **Cookie** — 请求配置 `credentials: 'include'`

```ts
fetch(`${BASE_URL}/api/traffic`, {
  headers: { Authorization: `Bearer ${token}` },
  credentials: 'include',
});
```

---

## 字段契约

为兼容后端 Python（snake_case）与前端 JS（camelCase）的命名差异，数据层遵循以下约定：

| 规则 | 说明 |
|:-----|:-----|
| **读取优先** `snake_case` | 后端返回的字段统一使用 snake_case |
| **兼容读取** `camelCase` | 过渡期同时支持 camelCase，由 `normalize.ts` 统一转换 |

> 新增字段请始终使用 **snake_case**，camelCase 兼容将在后续版本移除。
