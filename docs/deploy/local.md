# 本地开发部署

目标：在本机完成 backend + frontend 联调，必要时再单独启动 edge。

## 1. 前置依赖

- Node.js 20+
- pnpm 9+
- Java 17+
- Maven 3.9+
- Python 3.10+
- Docker Compose

## 2. 启动数据库与缓存

在项目根目录执行：

```bash
docker compose up -d database mysql redis
docker compose ps database mysql redis
```

预期：
- `database`、`mysql`、`redis` 全部为 `healthy`

## 3. 启动 backend

`backend/src/main/resources/application.yml` 的默认值是面向纯本地运行的；
如果你使用仓库自带 Compose 起依赖，需要显式覆盖数据库名、密码和 Redis 端口。

```bash
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/transportation_system \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=odoo \
SPRING_REDIS_HOST=localhost \
SPRING_REDIS_PORT=6380 \
JWT_SECRET=change-this-dev-secret-change-this-dev-secret \
mvn -B spring-boot:run
```

验证：

```bash
curl http://localhost:8000/api/v1/site-settings
```

## 4. 启动 frontend

```bash
cd frontend
pnpm install
pnpm dev
```

说明：
- Vite 默认监听 `0.0.0.0:5174`
- 开发模式下已内置代理：
  - `/api` -> `http://127.0.0.1:8000`
  - `/api/v1/ws` -> `ws://127.0.0.1:8000`

访问：
- `http://localhost:5174`

## 5. 启动 edge（可选）

仿真模式：

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py --mode sim --port 8000 --no-browser
```

摄像头模式：

```bash
python main.py --mode camera --url 0 --road "人民路" --port 8000 --no-browser
```

说明：
- edge 默认端口是 `8000`，不是 `9000`
- 无头环境请显式加 `--no-browser`
- 生产/容器环境使用 `camera` 模式时，必须设置 `--url` 或 `CAMERA_URL`，否则会进入交互选择

## 6. 本地验收

```bash
curl -I http://localhost:8000/api/v1/site-settings
curl -I http://localhost:5174
```

如果 edge 已启动：

```bash
curl http://localhost:8000/health
curl http://localhost:8000/api/metrics
```

## 7. 本地门禁

回到仓库根目录执行：

```bash
./scripts/local-gate.sh
```

门禁顺序：
- `backend`：`mvn -B test`
- `edge`：`python -m py_compile *.py` + `pytest -q tests`
- `frontend`：`pnpm test` + `pnpm build`
