#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TABLES=(traffic_samples traffic_events traffic_predictions)
SINCE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all)
      TABLES=(users cameras site_settings traffic_samples traffic_events traffic_predictions api_clients)
      shift
      ;;
    --since)
      if [[ $# -lt 2 ]]; then
        echo "[consistency] ERROR: --since 需要提供 ISO 时间，例如 2026-02-27T03:00:00"
        exit 2
      fi
      SINCE="$2"
      shift 2
      ;;
    *)
      echo "[consistency] ERROR: 不支持参数 $1"
      echo "用法: $0 [--all] [--since 2026-02-27T03:00:00]"
      exit 2
      ;;
  esac
done

echo "[consistency] compare postgres(database) vs mysql(mysql)"
echo "[consistency] tables: ${TABLES[*]}"
if [[ -n "$SINCE" ]]; then
  echo "[consistency] mode: incremental since $SINCE"
fi

build_count_sql() {
  local table="$1"
  local engine="$2"
  local where_clause=""

  if [[ -n "$SINCE" ]]; then
    case "$table" in
      traffic_samples|traffic_events|traffic_predictions)
        if [[ "$engine" == "postgres" ]]; then
          where_clause=" WHERE created_at >= '${SINCE}'::timestamp"
        else
          where_clause=" WHERE created_at >= '${SINCE}'"
        fi
        ;;
    esac
  fi

  printf "SELECT COUNT(*) FROM %s%s;" "$table" "$where_clause"
}

status=0
for table in "${TABLES[@]}"; do
  pg_sql="$(build_count_sql "$table" "postgres")"
  my_sql="$(build_count_sql "$table" "mysql")"
  pg_count=$(docker compose exec -T database psql -U postgres -d transportation_system -Atc "$pg_sql" 2>/dev/null | tr -d '\r' || echo "ERR")
  my_count=$(docker compose exec -T mysql mysql -utraffic -ptraffic -D transportation_system_mysql -Nse "$my_sql" 2>/dev/null | tr -d '\r' || echo "ERR")

  printf "%-22s postgres=%-10s mysql=%-10s\n" "$table" "$pg_count" "$my_count"

  if [[ "$pg_count" == "ERR" || "$my_count" == "ERR" ]]; then
    status=2
    continue
  fi

  if [[ "$pg_count" != "$my_count" ]]; then
    status=1
  fi
done

if [[ "$status" -eq 0 ]]; then
  echo "[consistency] OK"
elif [[ "$status" -eq 1 ]]; then
  echo "[consistency] WARNING: count mismatch detected"
else
  echo "[consistency] ERROR: failed to query one or more tables"
fi

exit "$status"
