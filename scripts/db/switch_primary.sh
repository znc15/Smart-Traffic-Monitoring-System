#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

target="${1:-}"
if [[ "$target" != "postgres" && "$target" != "mysql" ]]; then
  echo "用法: $0 <postgres|mysql>"
  echo "示例: $0 postgres   # 主库=PostgreSQL, 镜像=MySQL"
  echo "示例: $0 mysql      # 主库=MySQL, 镜像=PostgreSQL"
  exit 1
fi

common_env=(
  "APP_DB_MIRROR_WRITE=true"
  "APP_DB_MIRROR_POSTGRES_URL=jdbc:postgresql://database:5432/transportation_system"
  "APP_DB_MIRROR_POSTGRES_USERNAME=postgres"
  "APP_DB_MIRROR_POSTGRES_PASSWORD=odoo"
  "APP_DB_MIRROR_MYSQL_URL=jdbc:mysql://mysql:3306/transportation_system_mysql?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
  "APP_DB_MIRROR_MYSQL_USERNAME=traffic"
  "APP_DB_MIRROR_MYSQL_PASSWORD=traffic"
)

if [[ "$target" == "postgres" ]]; then
  echo "[db-switch] 切到 PostgreSQL 主库（灰度双写 MySQL）"
  docker compose exec -T mysql mysql -utraffic -ptraffic -D transportation_system_mysql \
    -e "DELETE FROM flyway_schema_history WHERE success = 0;" >/dev/null 2>&1 || true
  env "${common_env[@]}" \
    APP_DB_PRIMARY=postgres \
    APP_DB_MIRROR_MYSQL_ENABLED=true \
    APP_DB_MIRROR_POSTGRES_ENABLED=false \
    SPRING_FLYWAY_LOCATIONS=classpath:db/migration \
    SPRING_DATASOURCE_URL=jdbc:postgresql://database:5432/transportation_system \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_PASSWORD=odoo \
    docker compose up -d backend
else
  echo "[db-switch] 切到 MySQL 主库（灰度双写 PostgreSQL）"
  docker compose exec -T mysql mysql -utraffic -ptraffic -D transportation_system_mysql \
    -e "DELETE FROM flyway_schema_history WHERE success = 0;" >/dev/null 2>&1 || true
  env "${common_env[@]}" \
    APP_DB_PRIMARY=mysql \
    APP_DB_MIRROR_MYSQL_ENABLED=false \
    APP_DB_MIRROR_POSTGRES_ENABLED=true \
    SPRING_FLYWAY_LOCATIONS=classpath:db/migration-mysql \
    "SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/transportation_system_mysql?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" \
    SPRING_DATASOURCE_USERNAME=traffic \
    SPRING_DATASOURCE_PASSWORD=traffic \
    docker compose up -d backend
fi

echo "[db-switch] 后端重建完成，等待健康状态..."
for _ in $(seq 1 30); do
  status_line="$(docker compose ps --format json 2>/dev/null | grep '"Service":"backend"' | tail -n 1 || true)"
  if echo "$status_line" | grep -q '"Health":"healthy"'; then
    echo "[db-switch] backend 健康"
    exit 0
  fi
  if echo "$status_line" | grep -q '"State":"exited"'; then
    echo "[db-switch] backend 已退出，请查看 docker compose logs --no-color --tail=200 backend"
    exit 3
  fi
  sleep 2
done

echo "[db-switch] 警告：backend 未在 60 秒内进入 healthy，请执行 docker compose logs -f backend 排查"
exit 2
