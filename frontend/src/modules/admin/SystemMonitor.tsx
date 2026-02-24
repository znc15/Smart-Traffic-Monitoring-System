import { useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { useWebSocket } from "@/hooks/useWebSocket";
import { getApiUrl, getWsUrl } from "@/config/settings";
import { authConfig } from "@/config";
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

type Metrics = {
  cpu_percent: number | null;
  memory: { total: number; available: number; percent: number; used: number; free: number } | null;
  disk: { total: number; used: number; free: number; percent: number } | null;
  gpu: unknown;
  error?: string;
};

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
