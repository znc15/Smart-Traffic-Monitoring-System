# Smart Traffic Frontend

Vue 3 管理端，提供登录、交通态势概览、实时状态推送、历史数据统计与节点配置管理界面。

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Vue 3.5 |
| 语言 | TypeScript 5 |
| 构建 | Vite 5 |
| UI | Naive UI |
| 可视化 | ECharts |
| 路由 | Vue Router 4 |
| 部署 | Nginx + Docker |

## 常用命令

```bash
cd frontend
pnpm install
pnpm dev
pnpm build
pnpm test
pnpm preview
pnpm lint
pnpm lint:fix
pnpm format
```

说明：
- 开发模式默认监听 `0.0.0.0:5174`
- Node.js 建议使用 `20+`

## 本地开发

```bash
cd frontend
pnpm install
pnpm dev
```

开发代理已内置在 `vite.config.ts`：
- `/api` -> `http://127.0.0.1:8000`
- `/api/v1/ws` -> `ws://127.0.0.1:8000`

访问：
- `http://localhost:5174`

## 路由概览

| 路径 | 说明 |
|------|------|
| `/login` | 登录 / 注册 |
| `/dashboard` | 交通态势概览 |
| `/cameras` | 实时视频监测 |
| `/monitoring` | 节点监控汇聚 |
| `/map` | 实时状态推送 |
| `/analytics` | 历史数据统计 |
| `/alerts` | 异常事件告警 |
| `/admin` | 节点配置管理 |
| `/ai-assistant` | AI 智能分析 |

## 环境变量

完整模板见 [`frontend/.env.example`](.env.example)。

| 变量 | 说明 |
|------|------|
| `VITE_API_HTTP_BASE` | HTTP API 基地址 |
| `VITE_API_WS_BASE` | WebSocket 基地址 |
| `VITE_AMAP_KEY` | 高德地图 Web JS API Key（部署 fallback） |
| `VITE_AMAP_SECURITY_JS_CODE` | 高德 JSAPI 安全密钥（可选） |
| `VITE_AMAP_SERVICE_HOST` | 高德安全代理地址（可选，优先级高于 `VITE_AMAP_SECURITY_JS_CODE`） |

重要说明：
- `VITE_*` 是 build-time config（构建时注入）
- 修改后必须重新执行 `pnpm build` 或重新构建 Docker 镜像
- 运行中的 Nginx 容器不会读取 `docker run -e VITE_*`
- 如果你通过仓库根 `docker-compose.yml` 构建主站，frontend 默认从根 `.env` 的 `BACKEND_PUBLIC_HTTP_BASE / BACKEND_PUBLIC_WS_BASE` 取值
- 地图页优先读取后台“站点设置”里的 `amap_key`；只有后台留空时，才回退到 `VITE_AMAP_KEY`
- 高德安全配置只支持前端环境变量入口，不走后台“站点设置”
- 如果同时配置 `VITE_AMAP_SERVICE_HOST` 和 `VITE_AMAP_SECURITY_JS_CODE`，前者优先生效

### 同源部署（推荐）

如果前端通过 `gateway` 暴露，并且 `/api`、`/api/v1/ws` 也走同一个域名：

```env
VITE_API_HTTP_BASE=
VITE_API_WS_BASE=
VITE_AMAP_KEY=your_amap_key
VITE_AMAP_SECURITY_JS_CODE=
VITE_AMAP_SERVICE_HOST=
```

留空后，前端会自动回退到当前页面同源地址。高德地图 Key 也可改为在后台“站点设置”中维护。

### 跨域部署

```env
VITE_API_HTTP_BASE=http://192.168.1.11:8000
VITE_API_WS_BASE=ws://192.168.1.11:8000
VITE_AMAP_KEY=your_amap_key
VITE_AMAP_SECURITY_JS_CODE=
VITE_AMAP_SERVICE_HOST=
```

如果你走主站 Docker 一键启动，优先修改根 `.env`：

```env
GATEWAY_PUBLIC_BASE=http://192.168.1.10:5173
BACKEND_PUBLIC_HTTP_BASE=http://192.168.1.11:8000
BACKEND_PUBLIC_WS_BASE=ws://192.168.1.11:8000
```

然后执行：

```bash
docker compose build frontend gateway
docker compose up -d frontend gateway
```

## 生产构建

```bash
cd frontend
pnpm build
```

产物输出到 `dist/`，由 `frontend/nginx.conf` 托管。

当前生产 Nginx 行为：
- `try_files $uri $uri/ /index.html`，支持 SPA 路由直达
- `/assets/` 开启静态缓存
- `index.html` 关闭缓存

Docker 构建：

```bash
cd frontend
docker build -t smart-traffic-frontend .
docker run -p 8080:80 smart-traffic-frontend
```

## 鉴权方式

前端请求默认会：
- 在 `Authorization` 头中带 Bearer Token
- 同时启用 `credentials: 'include'` 发送 Cookie

这样可以兼容：
- 直接 Bearer Token 鉴权
- 服务端写入的 HttpOnly Cookie

## 字段契约

后端返回字段以 `snake_case` 为主，前端通过 `src/lib/normalize.ts` 做兼容读取。

约定：
- 新字段优先使用 `snake_case`
- 历史 `camelCase` 仅做兼容，不建议继续新增
