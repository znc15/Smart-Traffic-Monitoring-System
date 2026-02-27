#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_DIR="$ROOT_DIR/docs/reports"
RAW_DIR="$REPORT_DIR/raw"
mkdir -p "$RAW_DIR"

EDGE_URL="${EDGE_URL:-http://localhost:9000}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8000}"
ROAD_NAME="${ROAD_NAME:-陈兴道路}"
SAMPLES="${SAMPLES:-60}"
SKIP_EDGE="${SKIP_EDGE:-false}"

now_ts="$(date +%Y%m%d_%H%M%S)"
edge_csv="$RAW_DIR/edge_metrics_${now_ts}.csv"
lat_csv="$RAW_DIR/backend_latency_${now_ts}.csv"
summary_md="$REPORT_DIR/perf-summary-${now_ts}.md"

echo "timestamp,fps,inference_ms,cpu_percent,memory_percent" > "$edge_csv"
edge_enabled=true
if [[ "$SKIP_EDGE" == "true" ]]; then
  edge_enabled=false
else
  if ! curl -fsS "$EDGE_URL/api/metrics" >/dev/null 2>&1; then
    edge_enabled=false
  fi
fi

if [[ "$edge_enabled" == "true" ]]; then
  for _ in $(seq 1 "$SAMPLES"); do
    json="$(curl -s "$EDGE_URL/api/metrics" || true)"
    python3 - <<'PY' "$json" "$edge_csv"
import csv, json, sys, datetime
payload = {}
try:
    payload = json.loads(sys.argv[1])
except Exception:
    payload = {}
path = sys.argv[2]
with open(path, 'a', newline='') as f:
    w = csv.writer(f)
    w.writerow([
        datetime.datetime.now().isoformat(),
        payload.get('fps', ''),
        payload.get('inference_ms', ''),
        payload.get('cpu_percent', ''),
        payload.get('memory_percent', ''),
    ])
PY
    sleep 1
  done
else
  echo "[perf] edge metrics skipped (SKIP_EDGE=$SKIP_EDGE, EDGE_URL=$EDGE_URL)"
fi

echo "timestamp,latency_ms" > "$lat_csv"
for _ in $(seq 1 "$SAMPLES"); do
  start="$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)"
  curl -s "$BACKEND_URL/api/v1/traffic/predictions?road_name=$ROAD_NAME&horizon_minutes=30" >/dev/null || true
  end="$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)"
  echo "$(date --iso-8601=seconds 2>/dev/null || date +%Y-%m-%dT%H:%M:%S),$((end-start))" >> "$lat_csv"
  sleep 1
done

python3 - <<'PY' "$edge_csv" "$lat_csv" "$summary_md" "$edge_enabled"
import csv, statistics, sys
edge_csv, lat_csv, summary, edge_enabled = sys.argv[1:5]

fps, infer = [], []
with open(edge_csv) as f:
    r = csv.DictReader(f)
    for row in r:
        try:
            if row['fps'] != '': fps.append(float(row['fps']))
            if row['inference_ms'] != '': infer.append(float(row['inference_ms']))
        except Exception:
            pass

lat = []
with open(lat_csv) as f:
    r = csv.DictReader(f)
    for row in r:
        try:
            lat.append(float(row['latency_ms']))
        except Exception:
            pass

p95 = statistics.quantiles(lat, n=20)[18] if len(lat) >= 20 else (max(lat) if lat else 0)
avg_fps = sum(fps)/len(fps) if fps else 0
avg_infer = sum(infer)/len(infer) if infer else 0

with open(summary, 'w') as f:
    f.write('# 性能证据包汇总\n\n')
    if edge_enabled == 'true' and fps:
        f.write(f'- edge 平均 FPS: {avg_fps:.2f}\n')
        f.write(f'- edge 平均 inference_ms: {avg_infer:.2f}\n')
    else:
        f.write('- edge 平均 FPS: N/A（本次未采集 edge 指标）\n')
        f.write('- edge 平均 inference_ms: N/A（本次未采集 edge 指标）\n')
    f.write(f'- backend 预测接口 P95 延迟: {p95:.2f} ms\n')
    f.write(f'- 原始 edge 指标: `{edge_csv}`\n')
    f.write(f'- 原始延迟数据: `{lat_csv}`\n')

print(summary)
PY

echo "[perf] summary generated: $summary_md"
