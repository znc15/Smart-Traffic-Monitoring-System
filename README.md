# 智能交通监控系统（Smart Transportation Monitoring System）

本项目是一个智能交通监控系统：

- 边缘节点基于 YOLOv8 + OpenVINO 进行实时车辆检测，内置 Web 仪表盘
- 后端采用 Spring Boot 架构，汇聚多节点数据，提供 `/api/v1` 接口
- 前端实时展示道路车流数据、视频帧与系统资源使用情况

## 一、技术架构

- **边缘节点**：Python + FastAPI + YOLOv8 + OpenVINO，内置 Web UI 仪表盘
- **后端**：Spring Boot 3 + Spring Security + Spring WebSocket + Spring Data JPA
- **前端**：React + Vite
- **数据库**：PostgreSQL 16
- **接口基础路径**：后端 `http://localhost:8000/api/v1`，边缘节点 `http://localhost:8000/api/`

## 二、项目结构

```text
Smart-Traffic-Monitoring-System
├── edge/                      # 边缘检测节点（YOLOv8 + FastAPI）
│   ├── main.py                #   应用入口，CLI 参数解析
│   ├── config.py              #   配置（CLI > 环境变量 > 默认值）
│   ├── detector.py            #   YOLOv8 模型加载与车辆检测
│   ├── routes.py              #   FastAPI 路由定义
│   ├── loops.py               #   摄像头 / 模拟后台循环
│   ├── state.py               #   线程安全全局状态
│   └── static/                #   Web UI 仪表盘
├── backend/                   # 后端服务（Spring Boot）
│   └── src/main/java/...
│       ├── controller/        #   REST 控制器
│       ├── service/           #   业务逻辑
│       ├── websocket/         #   WebSocket 处理器
│       └── security/          #   JWT 认证
├── frontend/                  # 前端应用（React + Vite）
│   └── src/
│       ├── modules/           #   功能模块（交通、视频、管理）
│       └── pages/             #   页面组件
├── docker-compose.yml
└── README.md
```

## 三、环境要求

- Python 3.10+（边缘节点）
- Java 17+（后端）
- Maven 3.9+（后端）
- Node.js 18+（前端）
- Docker / Docker Compose（可选）

## 四、使用 Docker 启动（推荐）

```bash
docker compose up --build
```

启动后服务地址：

- 后端 API：`http://localhost:8000`
- 前端：`http://localhost:5173`
- PostgreSQL：`localhost:5433`

停止服务：

```bash
docker compose down
```

## 五、本地启动（不使用 Docker）

### 1) 启动边缘节点

```bash
cd edge
pip install -r requirements.txt
python main.py                # 模拟模式，自动打开 Web 仪表盘
```

启动后浏览器会自动打开仪表盘（`http://localhost:8000`），可通过 `--no-browser` 禁用。

```bash
# 摄像头模式
python main.py --mode camera --url 0

# 自定义端口和路段名
python main.py --port 9001 --road "长安街-East"

# Docker/无头环境
python main.py --no-browser
```

> 详细 CLI 参数和 API 文档见 [edge/README.md](edge/README.md)。

### 2) 启动后端

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### 3) 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

## 六、环境变量

### 后端（`backend/src/main/resources/application.yml`）

- `SERVER_PORT`（默认：`8000`）
- `SPRING_DATASOURCE_URL`（默认：`jdbc:postgresql://localhost:5433/traffic_db`）
- `SPRING_DATASOURCE_USERNAME`（默认：`postgres`）
- `SPRING_DATASOURCE_PASSWORD`（默认：`password`）
- `JWT_SECRET`
- `ACCESS_TOKEN_EXPIRE_DAYS`（默认：`7`）
- `BASE_URL_API`（默认：`http://localhost:8000`）
- `TRAFFIC_ROADS`（逗号分隔的道路名称列表）

### 边缘节点（`edge/config.py`）

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MODE` | 运行模式: `sim` / `camera` | `sim` |
| `CAMERA_URL` | 视频源地址（RTSP URL / 设备编号） | 空（交互选择） |
| `ROAD_NAME` | 路段名称 | `示例路段-Edge01` |
| `MODEL` | YOLOv8 模型文件名 | `yolov8n.pt` |
| `CONF` | 检测置信度阈值 | `0.35` |
| `PORT` | HTTP 服务端口 | `8000` |
| `OPENVINO` | 启用 OpenVINO 加速 | `true` |
| `NO_BROWSER` | 禁用自动打开浏览器 | `false` |

> CLI 参数优先级高于环境变量，详见 [edge/README.md](edge/README.md)。

## 七、API 接口总览（`/api/v1`）

### 1) 认证接口（Auth）

- `POST /auth/register`（JSON：`username,email,phone_number,password`）
- `POST /auth/login`（`form-urlencoded`：`username=<邮箱>&password=...`）
- `GET /auth/me`（需要 JWT）

### 2) 用户接口（User）

- `PUT /users/profile`（需要 JWT）
- `PUT /users/password`（需要 JWT）

### 3) 交通接口（Traffic）

- `GET /roads_name`
- `GET /info/{road_name}`
- `GET /frames/{road_name}`（需要 JWT）
- `GET /frames_no_auth/{road_name}`
- `WS /ws/info/{road_name}`（需要 JWT）
- `WS /ws/frames/{road_name}`（需要 JWT）

### 4) 管理接口（Admin）

- `GET /admin/resources`（需要 JWT + 管理员角色）
- `WS /admin/ws/resources`（需要 JWT + 管理员角色）
- `GET /admin/cameras`（需要 JWT + 管理员角色）
- `POST /admin/cameras`（需要 JWT + 管理员角色）
- `PUT /admin/cameras/{id}`（需要 JWT + 管理员角色）
- `DELETE /admin/cameras/{id}`（需要 JWT + 管理员角色）
- `GET /admin/users`（需要 JWT + 管理员角色）
- `PUT /admin/users/{id}/role`（需要 JWT + 管理员角色）
- `PUT /admin/users/{id}/status`（需要 JWT + 管理员角色）

### 5) 站点设置（Site Settings）

- `GET /site-settings`（公开）
- `PUT /admin/site-settings`（需要 JWT + 管理员角色）

## 八、鉴权说明

- REST 请求：`Authorization: Bearer <token>`
- WebSocket：可通过 `Authorization` 头、`access_token` Cookie 或 `?token=` 传递
- 登录成功后，后端也会写入 `httpOnly` 的 `access_token` Cookie

## 九、快速调用示例

```bash
# 登录（form-urlencoded）
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com&password=your_password"
```

## 十、边缘节点 API（Edge Node API）

边缘节点基于 FastAPI，启动后自动打开 Web 仪表盘。后端 `CameraPollerService` 每 3 秒轮询已配置 `stream_url` 的节点。

### 数据接口（后端轮询）

- `GET /api/traffic` — 交通数据 + 节点性能指标
- `GET /api/frame` — 最新 JPEG 检测帧
- `GET /api/metrics` — 独立性能指标端点
- `GET /api/stream` — MJPEG 实时视频流（最多 5 个并发客户端）
- `GET /health` — 健康检查

### 配置管理接口（运行时热切换）

- `GET /api/config` — 读取当前运行配置
- `PUT /api/config` — 更新配置并热重启检测循环（支持切换模式/摄像头源/模型/置信度/路段名）
- `GET /api/models` — 列出可用模型文件

### 文件上传检测接口

- `POST /api/detect/image` — 上传图片进行车辆检测（返回标注图片 + 检测结果）
- `POST /api/detect/video` — 上传视频进行逐帧检测（返回标注视频下载链接）
- `GET /api/detect/video/result/{id}` — 下载检测结果视频

### Web 仪表盘

启动后访问 `http://localhost:<port>` 自动进入深色主题监控面板，包含三个功能页：

- 监控 Tab：实时 MJPEG 视频流、车辆统计、系统指标、趋势图表
- 设置 Tab：运行时热切换摄像头源、模型、置信度、路段名等配置
- 测试 Tab：拖拽上传图片/视频文件进行检测测试

### 接入后端

在管理面板「摄像头管理」中设置每个摄像头的「接入地址」（如 `http://192.168.1.100:8080`），留空则使用模拟数据。超时设置：连接 2 秒，读取 2 秒；轮询间隔：3 秒。
