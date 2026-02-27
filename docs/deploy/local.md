# 本地部署教程（Local）

本教程目标：从零启动后，可访问页面并完成接口验证。

## 1. 前置依赖

- Node.js 20+
- pnpm 9+
- Java 17+
- Maven 3.9+
- Python 3.10+
- Docker + Docker Compose

## 2. 启动基础依赖（数据库与缓存）

项目根目录执行：

```bash
docker compose up -d database mysql redis
```

验证：

```bash
docker compose ps database mysql redis
```

期望状态：3 个服务均为 `healthy`。

## 3. 启动后端

```bash
cd backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/transportation_system \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=odoo \
JWT_SECRET=change-this-dev-secret-change-this-dev-secret \
mvn -B spring-boot:run
```

验证：

```bash
curl http://localhost:8000/api/v1/site-settings
```

## 4. 启动 Vue 前端

```bash
cd frontend
pnpm install
pnpm dev --port 5174
```

浏览器访问：`http://localhost:5174`

## 5. 启动 Edge（可选）

```bash
cd edge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py --mode sim --port 9000 --no-browser
```

## 6. 本地门禁

项目根目录执行：

```bash
./scripts/local-gate.sh
```
