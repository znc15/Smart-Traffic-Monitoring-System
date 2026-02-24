import { useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Badge } from "@/ui/badge";
import { useWebSocket } from "@/hooks/useWebSocket";
import { getApiUrl, getWsUrl } from "@/config/settings";
import { authConfig } from "@/config";
import { Server, Cpu, Timer, Activity } from "lucide-react";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from "recharts";

type NodeInfo = {
  name: string;
  online: boolean;
  last_success_time: string | null;
  last_poll_time: string | null;
  latency_ms: number;
  error_count: number;
  consecutive_failures: number;
  last_error: string | null;
  edge_metrics?: {
    cpu_percent: number;
    memory_percent: number;
    memory_used: number;
    memory_total: number;
    gpu_percent: number | null;
    gpu_memory_percent: number | null;
    inference_ms: number;
    fps: number;
    uptime_s: number;
    model: string;
  } | null;
};

type Metrics = {
  cpu_percent: number | null;
  process_cpu: number | null;
  memory: { total: number; available: number; percent: number; used: number; free: number } | null;
  disk: { total: number; used: number; free: number; percent: number } | null;
  jvm: {
    heap_used: number;
    heap_max: number;
    heap_percent: number;
    non_heap_used: number;
    thread_count: number;
    uptime_ms: number;
    gc_count: number;
    gc_time_ms: number;
  } | null;
  gpu: unknown;
  nodes?: Record<string, NodeInfo>;
  error?: string;
};

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const val = bytes / Math.pow(1024, i);
  return `${val.toFixed(1)} ${units[i]}`;
}

function formatUptime(ms: number): string {
  const totalMin = Math.floor(ms / 60000);
  const d = Math.floor(totalMin / 1440);
  const h = Math.floor((totalMin % 1440) / 60);
  const m = totalMin % 60;
  const parts: string[] = [];
  if (d > 0) parts.push(`${d}d`);
  if (h > 0) parts.push(`${h}h`);
  parts.push(`${m}m`);
  return parts.join(" ");
}

export default function SystemMonitor() {
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [history, setHistory] = useState<{ time: string; cpu: number; mem: number; disk: number }[]>([]);
  const [lastUpdate, setLastUpdate] = useState<string | null>(null);

  const token = useMemo(
    () => (typeof window !== "undefined" ? localStorage.getItem(authConfig.TOKEN_KEY) : null),
    []
  );

  const fetchMetrics = async () => {
    if (!token) return;
    try {
      const res = await fetch(getApiUrl("/admin/resources"), {
        headers: { Authorization: `Bearer ${token}` },
        credentials: "include",
      });
      if (res.ok) {
        setMetrics((await res.json()) as Metrics);
        setLastUpdate(new Date().toLocaleTimeString("zh-CN"));
      }
    } catch { /* ignore */ }
  };

  useEffect(() => { fetchMetrics(); }, []);

  const wsUrl = useMemo(() => getWsUrl("/admin/ws/resources"), []);
  const { data: wsData, isConnected } = useWebSocket(wsUrl, {
    authToken: token,
    maxReconnectAttempts: 10,
  });

  useEffect(() => {
    if (wsData && typeof wsData === "object") {
      const m = wsData as Metrics;
      setMetrics(m);
      const point = {
        time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
        cpu: Number(m.cpu_percent || 0),
        mem: Number(m.memory?.percent || 0),
        disk: Number(m.disk?.percent || 0),
      };
      setHistory((prev) => [...prev, point].slice(-60));
      setLastUpdate(new Date().toLocaleTimeString("zh-CN"));
    }
  }, [wsData]);

  useEffect(() => {
    if (!metrics) return;
    setHistory((prev) => {
      if (prev.length > 0) return prev;
      return [{
        time: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit" }),
        cpu: Number(metrics.cpu_percent || 0),
        mem: Number(metrics.memory?.percent || 0),
        disk: Number(metrics.disk?.percent || 0),
      }];
    });
  }, [metrics]);

  const StatCard = ({ label, value, color }: { label: string; value: number; color: string }) => (
    <Card className="p-4 shadow-sm border-border/50">
      <div className="flex items-center justify-between">
        <div>
          <div className="text-sm text-muted-foreground">{label}</div>
          <div className="text-2xl font-bold text-foreground">{value}%</div>
        </div>
        <div className="w-24 h-6 bg-secondary rounded-full overflow-hidden">
          <div className={`h-full ${color} rounded-full transition-all duration-500`} style={{ width: `${value}%` }} />
        </div>
      </div>
    </Card>
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="text-sm text-muted-foreground">
          WebSocket: {isConnected
            ? <span className="text-emerald-600 dark:text-emerald-400">已连接</span>
            : <span className="text-destructive">未连接</span>}
        </div>
        <Button variant="ghost" onClick={() => fetchMetrics()}>刷新</Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard label="CPU" value={metrics?.cpu_percent ?? 0} color="bg-blue-500 dark:bg-blue-400" />
        <StatCard label="RAM" value={metrics?.memory?.percent ?? 0} color="bg-emerald-500 dark:bg-emerald-400" />
        <StatCard label="Disk" value={metrics?.disk?.percent ?? 0} color="bg-amber-500 dark:bg-amber-400" />
      </div>

      {/* JVM Info Card */}
      {metrics?.jvm && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Server className="h-5 w-5" />
              JVM 运行状态
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Heap Memory */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  堆内存
                </div>
                <div className="text-sm font-medium">
                  {formatBytes(metrics.jvm.heap_used)} / {formatBytes(metrics.jvm.heap_max)}
                </div>
                <div className="w-full h-2 bg-secondary rounded-full overflow-hidden">
                  <div
                    className="h-full bg-violet-500 dark:bg-violet-400 rounded-full transition-all duration-500"
                    style={{ width: `${metrics.jvm.heap_percent}%` }}
                  />
                </div>
                <div className="text-xs text-muted-foreground">{metrics.jvm.heap_percent.toFixed(1)}%</div>
              </div>

              {/* Non-Heap Memory */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Activity className="h-4 w-4" />
                  非堆内存
                </div>
                <div className="text-sm font-medium">{formatBytes(metrics.jvm.non_heap_used)}</div>
              </div>

              {/* Process CPU */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Cpu className="h-4 w-4" />
                  进程 CPU
                </div>
                <div className="text-sm font-medium">{(metrics.process_cpu ?? 0).toFixed(1)}%</div>
              </div>

              {/* Thread Count */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Cpu className="h-4 w-4" />
                  线程数
                </div>
                <div className="text-sm font-medium">{metrics.jvm.thread_count}</div>
              </div>

              {/* Uptime */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Timer className="h-4 w-4" />
                  运行时间
                </div>
                <div className="text-sm font-medium">{formatUptime(metrics.jvm.uptime_ms)}</div>
              </div>

              {/* GC */}
              <div className="space-y-1.5">
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Timer className="h-4 w-4" />
                  GC 统计
                </div>
                <div className="text-sm font-medium">
                  {metrics.jvm.gc_count} 次 / {metrics.jvm.gc_time_ms} ms
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Camera Node Health Table */}
      <Card>
        <CardHeader>
          <CardTitle>摄像头节点状态</CardTitle>
        </CardHeader>
        <CardContent>
          {metrics?.nodes && Object.keys(metrics.nodes).length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-muted-foreground">
                    <th className="pb-2 pr-4 font-medium">名称</th>
                    <th className="pb-2 pr-4 font-medium">状态</th>
                    <th className="pb-2 pr-4 font-medium">延迟</th>
                    <th className="pb-2 pr-4 font-medium">最后成功时间</th>
                    <th className="pb-2 pr-4 font-medium">错误次数</th>
                    <th className="pb-2 font-medium">最近错误</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.entries(metrics.nodes).map(([key, node]) => (
                    <tr key={key} className="border-b border-border/50 last:border-0">
                      <td className="py-2.5 pr-4 font-medium">{node.name}</td>
                      <td className="py-2.5 pr-4">
                        {node.online ? (
                          <Badge className="bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/25">
                            在线
                          </Badge>
                        ) : (
                          <Badge className="bg-red-500/15 text-red-600 dark:text-red-400 border-red-500/25">
                            离线
                          </Badge>
                        )}
                      </td>
                      <td className="py-2.5 pr-4">{node.latency_ms} ms</td>
                      <td className="py-2.5 pr-4 text-muted-foreground">
                        {node.last_success_time
                          ? new Date(node.last_success_time).toLocaleString("zh-CN")
                          : "-"}
                      </td>
                      <td className="py-2.5 pr-4">{node.error_count}</td>
                      <td className="py-2.5 text-muted-foreground max-w-[200px] truncate">
                        {node.last_error ?? "-"}
                      </td>
                    </tr>
                  )).flatMap((row, i) => {
                    const entries = Object.entries(metrics.nodes!);
                    const [, node] = entries[i];
                    if (!node.edge_metrics) return [row];
                    const em = node.edge_metrics;
                    return [
                      row,
                      <tr key={`${entries[i][0]}-edge`} className="border-b border-border/50 last:border-0">
                        <td colSpan={6} className="py-1.5 px-4 bg-muted/30">
                          <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                            <span>CPU: <span className="text-foreground font-medium">{em.cpu_percent}%</span></span>
                            <span>内存: <span className="text-foreground font-medium">{em.memory_percent}%</span></span>
                            <span>GPU: <span className="text-foreground font-medium">{em.gpu_percent != null ? `${em.gpu_percent}%` : "N/A"}</span></span>
                            <span>推理: <span className="text-foreground font-medium">{em.inference_ms}ms</span></span>
                            <span>FPS: <span className="text-foreground font-medium">{em.fps}</span></span>
                            <span>模型: <span className="text-foreground font-medium">{em.model}</span></span>
                            <span>运行: <span className="text-foreground font-medium">{formatUptime(em.uptime_s * 1000)}</span></span>
                          </div>
                        </td>
                      </tr>,
                    ];
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">暂无节点数据</p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>性能趋势</CardTitle>
          <div className="text-sm text-muted-foreground">
            {lastUpdate ? `更新时间：${lastUpdate}` : "暂无数据"}
          </div>
        </CardHeader>
        <CardContent className="px-2 sm:px-4">
          <ResponsiveContainer width="100%" height={380}>
            <LineChart data={history} margin={{ left: 12, right: 12 }}>
              <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
              <XAxis dataKey="time" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} domain={[0, 100]} unit="%" />
              <Tooltip
                formatter={(value) => [`${Number(value).toFixed(1)}%`, ""]}
                contentStyle={{
                  backgroundColor: "var(--card)",
                  border: "1px solid var(--border)",
                  borderRadius: 10,
                  color: "var(--foreground)",
                  boxShadow: "0 4px 24px oklch(0 0 0 / 8%)",
                }}
              />
              <Legend wrapperStyle={{ fontSize: "12px" }} />
              <Line type="monotone" dataKey="cpu" name="CPU %" stroke="#3B82F6" strokeWidth={2} dot={false} isAnimationActive={false} />
              <Line type="monotone" dataKey="mem" name="RAM %" stroke="#10B981" strokeWidth={2} dot={false} isAnimationActive={false} />
              <Line type="monotone" dataKey="disk" name="Disk %" stroke="#F59E0B" strokeWidth={2} dot={false} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {metrics?.error && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardHeader><CardTitle className="text-destructive">警告</CardTitle></CardHeader>
          <CardContent><p>{metrics.error}</p></CardContent>
        </Card>
      )}
    </div>
  );
}
