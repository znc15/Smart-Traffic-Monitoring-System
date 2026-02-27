# 本地部署教程（Local）

## 前置依赖

- Node.js 20.19+
- pnpm 9+
- Java 17+
- Maven 3.9+
- Python 3.10+
- PostgreSQL 16（或使用 Docker）

## 1. 启动数据库

推荐：

```bash
docker run -d --name traffic-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=odoo \
  -e POSTGRES_DB=transportation_system \
  -p 5433:5432 postgres:16
```

## 2. 启动后端

```bash
cd backend
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/transportation_system
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=odoo
export JWT_SECRET=change-this-dev-secret-change-this-dev-secret
mvn -B spring-boot:run
```

验证：

```bash
curl http://localhost:8000/api/v1/site-settings
```

## 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

访问：`http://localhost:5173`

## 4. 启动边缘端

```bash
cd edge
pip install -r requirements.txt
python main.py --mode sim --port 9000 --no-browser
```

可选启用主动上报：

```bash
export BACKEND_TELEMETRY_URL=http://localhost:8000/api/v1/edge/telemetry
export EDGE_NODE_ID=edge-local-01
python main.py --mode sim --port 9000 --no-browser
```

## 5. 一键门禁

```bash
./scripts/local-gate.sh
```
