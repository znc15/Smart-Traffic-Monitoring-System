#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORT_DIR="$ROOT_DIR/docs/reports"
RAW_DIR="$REPORT_DIR/raw"
mkdir -p "$RAW_DIR"

EDGE_URL="${EDGE_URL:-http://localhost:9000}"
EDGE_CONFIG_URL="${EDGE_CONFIG_URL:-$EDGE_URL/api/config}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8000}"
ROAD_NAME="${ROAD_NAME:-陈兴道路}"
SAMPLES="${SAMPLES:-30}"
SKIP_EDGE="${SKIP_EDGE:-false}"
EDGE_NODE_ID="${EDGE_NODE_ID:-edge-01}"
EDGE_KEY="${EDGE_KEY:-}"
COLLECT_OPENVINO_COMPARE="${COLLECT_OPENVINO_COMPARE:-true}"

now_ts="$(date +%Y%m%d_%H%M%S)"
lat_csv="$RAW_DIR/backend_latency_${now_ts}.csv"
summary_md="$REPORT_DIR/perf-summary-${now_ts}.md"

curl_edge() {
  local url="$1"
  shift || true
  local args=("$url")
  if [[ -n "$EDGE_KEY" ]]; then
    args=(-H "X-Edge-Key: $EDGE_KEY" -H "X-Edge-Node-Id: $EDGE_NODE_ID" "${args[@]}")
  fi
  curl -fsS "${args[@]}" "$@"
}

edge_enabled=true
if [[ "$SKIP_EDGE" == "true" ]]; then
  edge_enabled=false
else
  if ! curl_edge "$EDGE_URL/api/metrics" >/dev/null 2>&1; then
    edge_enabled=false
  fi
fi

collect_edge_metrics() {
  local label="$1"
  local csv_path="$RAW_DIR/edge_metrics_${label}_${now_ts}.csv"
  echo "timestamp,fps,inference_ms,cpu_percent,memory_percent" > "$csv_path"
  for _ in $(seq 1 "$SAMPLES"); do
    json="$(curl_edge "$EDGE_URL/api/metrics" || true)"
    python3 - <<'PY' "$json" "$csv_path"
import csv, json, sys, datetime
payload = {}
try:
    payload = json.loads(sys.argv[1])
except Exception:
    payload = {}
with open(sys.argv[2], 'a', newline='') as f:
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
  echo "$csv_path"
}

toggle_openvino() {
  local value="$1"
  curl_edge "$EDGE_CONFIG_URL" \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "{\"use_openvino\":$value}" >/dev/null
  sleep 3
}

declare -A EDGE_REPORTS=()
initial_openvino=""

if [[ "$edge_enabled" == "true" ]]; then
  if [[ "$COLLECT_OPENVINO_COMPARE" == "true" ]]; then
    initial_openvino="$(curl_edge "$EDGE_CONFIG_URL" | python3 - <<'PY'
import json, sys
try:
    print("true" if json.load(sys.stdin).get("use_openvino") else "false")
except Exception:
    print("")
PY
)"
    toggle_openvino true
    EDGE_REPORTS[openvino_on]="$(collect_edge_metrics openvino_on)"
    toggle_openvino false
    EDGE_REPORTS[openvino_off]="$(collect_edge_metrics openvino_off)"
    if [[ -n "$initial_openvino" ]]; then
      toggle_openvino "$initial_openvino"
    fi
  else
    EDGE_REPORTS[current]="$(collect_edge_metrics current)"
  fi
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
  curl -s "$BACKEND_URL/api/v1/info/$ROAD_NAME" >/dev/null || true
  end="$(python3 - <<'PY'
import time
print(int(time.time()*1000))
PY
)"
  echo "$(date --iso-8601=seconds 2>/dev/null || date +%Y-%m-%dT%H:%M:%S),$((end-start))" >> "$lat_csv"
  sleep 1
done

python3 - <<'PY' "$summary_md" "$lat_csv" "${RAW_DIR}" "${now_ts}" "${EDGE_REPORTS[current]-}" "${EDGE_REPORTS[openvino_on]-}" "${EDGE_REPORTS[openvino_off]-}"
import csv, statistics, sys
summary, lat_csv, raw_dir, now_ts, current_csv, on_csv, off_csv = sys.argv[1:8]

def summarize_edge(csv_path):
    fps, infer = [], []
    if not csv_path:
        return None
    with open(csv_path) as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                if row["fps"]:
                    fps.append(float(row["fps"]))
                if row["inference_ms"]:
                    infer.append(float(row["inference_ms"]))
            except Exception:
                pass
    if not fps and not infer:
        return None
    return {
        "avg_fps": sum(fps) / len(fps) if fps else 0.0,
        "avg_infer": sum(infer) / len(infer) if infer else 0.0,
        "csv": csv_path,
    }

lat = []
with open(lat_csv) as f:
    reader = csv.DictReader(f)
    for row in reader:
        try:
            lat.append(float(row["latency_ms"]))
        except Exception:
            pass

p95 = statistics.quantiles(lat, n=20)[18] if len(lat) >= 20 else (max(lat) if lat else 0.0)
avg_latency = sum(lat) / len(lat) if lat else 0.0

edge_sections = [
    ("current", summarize_edge(current_csv)),
    ("openvino_on", summarize_edge(on_csv)),
    ("openvino_off", summarize_edge(off_csv)),
]

with open(summary, "w") as f:
    f.write("# 性能证据包汇总\n\n")
    f.write(f"- 采样时间: `{now_ts}`\n")
    available = [(label, data) for label, data in edge_sections if data]
    if available:
      for label, data in available:
        f.write(f"- edge `{label}` 平均 FPS: {data['avg_fps']:.2f}\n")
        f.write(f"- edge `{label}` 平均 inference_ms: {data['avg_infer']:.2f}\n")
      on = dict(available).get("openvino_on")
      off = dict(available).get("openvino_off")
      if on and off and off["avg_infer"] > 0:
        speedup = off["avg_infer"] / on["avg_infer"] if on["avg_infer"] > 0 else 0
        f.write(f"- OpenVINO 推理加速比: {speedup:.2f}x\n")
    else:
      f.write("- edge 指标: N/A（本次未采集到 edge 数据）\n")
    f.write(f"- backend 预测接口平均延迟: {avg_latency:.2f} ms\n")
    f.write(f"- backend 预测接口 P95 延迟: {p95:.2f} ms\n")
    f.write(f"- backend 原始延迟数据: `{lat_csv}`\n")
    for _, data in available:
      f.write(f"- edge 原始指标: `{data['csv']}`\n")
    f.write("\n## 准确率记录建议\n\n")
    f.write("- 记录测试集来源、样本数量、标注方法。\n")
    f.write("- 分别记录 `car / motor / person` 的 Precision、Recall、mAP50。\n")
    f.write("- 对 OpenVINO on/off 使用同一视频源重复采样，避免数据漂移。\n")

print(summary)
PY

echo "[perf] summary generated: $summary_md"
