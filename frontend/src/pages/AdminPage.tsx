import { useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { useWebSocket } from "@/hooks/useWebSocket";
import { getApiUrl, getWsUrl } from "@/config/settings";
import { authConfig } from "@/config";
import { useNavigate } from "react-router-dom";
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
  memory: {
    total: number;
    available: number;
    percent: number;
    used: number;
    free: number;
  } | null;
  disk: {
    total: number;
    used: number;
    free: number;
    percent: number;
  } | null;
  gpu: unknown;
  error?: string;
};

// (no helper needed: chart shows percentages only)

export default function AdminPage() {
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null);
  const [history, setHistory] = useState<
    { time: string; cpu: number; mem: number; disk: number }[]
  >([]);
  const [lastUpdate, setLastUpdate] = useState<string | null>(null);
  const navigate = useNavigate();

  const token = useMemo(
    () =>
      typeof window !== "undefined"
        ? localStorage.getItem(authConfig.TOKEN_KEY)
        : null,
    []
  );

  // Centralized fetch so we can call it from refresh button or initial load
  const fetchMetrics = async () => {
    if (!token) return;
    try {
      const res = await fetch(getApiUrl("/admin/resources"), {
        headers: { Authorization: `Bearer ${token}` },
        credentials: "include",
      });
        if (res.ok) {
          const data = (await res.json()) as Metrics;
          setMetrics(data);
          setLastUpdate(new Date().toLocaleTimeString("zh-CN"));
        } else if (res.status === 403) {
          setError("仅管理员可访问");
        } else if (res.status === 401) {
          setError("请重新登录");
        } else {
          setError("无法加载系统数据");
        }
      } catch {
        setError("无法连接服务器");
      }
    };
  // Verify admin role before loading content
  useEffect(() => {
    let cancelled = false;
    const checkRole = async () => {
      try {
        if (!token) {
          setIsAdmin(false);
          setError("未登录");
          setLoading(false);
          return;
        }
        const res = await fetch(getApiUrl("/auth/me"), {
          headers: { Authorization: `Bearer ${token}` },
          credentials: "include",
        });
        if (!res.ok) {
          setIsAdmin(false);
          setError(
            res.status === 401
              ? "无访问权限"
              : "无法验证用户"
          );
          setLoading(false);
          return;
        }
        const me = await res.json();
        if (!cancelled) {
          const admin = me?.role_id === 0;
          setIsAdmin(admin);
          if (!admin) {
            setError("你无权访问此页面");
          }
        }
      } catch {
        setIsAdmin(false);
        setError("无法连接服务器");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    checkRole();
    return () => {
      cancelled = true;
    };
  }, [token]);

  // Initial fetch of metrics
  useEffect(() => {
    if (isAdmin) fetchMetrics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAdmin]);

  // Live updates via WebSocket
  const wsUrl = useMemo(() => getWsUrl("/admin/ws/resources"), []);
  const { data: wsData, isConnected } = useWebSocket(isAdmin ? wsUrl : null, {
    authToken: token,
    maxReconnectAttempts: 10,
  });

  useEffect(() => {
    if (wsData && typeof wsData === "object") {
      const m = wsData as Metrics;
      setMetrics(m);
      // Append to history
      const point = {
        time: new Date().toLocaleTimeString("zh-CN", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        }),
        cpu: Number(m.cpu_percent || 0),
        mem: Number(m.memory?.percent || 0),
        disk: Number(m.disk?.percent || 0),
      };
      setHistory((prev) => [...prev, point].slice(-60));
      setLastUpdate(new Date().toLocaleTimeString("zh-CN"));
    }
  }, [wsData]);

  // Seed first point from initial HTTP fetch if not yet seeded
  useEffect(() => {
    if (!metrics) return;
    setHistory((prev) => {
      if (prev.length > 0) return prev;
      const point = {
        time: new Date().toLocaleTimeString("zh-CN", {
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        }),
        cpu: Number(metrics.cpu_percent || 0),
        mem: Number(metrics.memory?.percent || 0),
        disk: Number(metrics.disk?.percent || 0),
      };
      return [point];
    });
  }, [metrics]);

  if (loading) {
    return (
      <div className="p-6">
        <p>加载中...</p>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="p-6">
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle>访问被拒绝</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="mb-4">
              {error || "你无权访问管理员页面。"}
            </p>
            <Button onClick={() => navigate("/home")}>返回首页</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">系统监控面板</h2>
        <div className="flex items-center gap-3">
          <div className="text-sm text-muted-foreground">
            WebSocket:{" "}
            {isConnected ? (
              <span className="text-emerald-600 dark:text-emerald-400">已连接</span>
            ) : (
              <span className="text-destructive">未连接</span>
            )}
          </div>
          <Button variant="ghost" onClick={() => fetchMetrics()}>
            刷新
          </Button>
        </div>
      </div>

      {/* Top quick stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="p-4 shadow-sm border-border/50">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-muted-foreground">CPU</div>
              <div className="text-2xl font-bold text-foreground">
                {metrics?.cpu_percent ?? 0}%
              </div>
            </div>
            <div className="w-24 h-6 bg-secondary rounded-full overflow-hidden">
              <div
                className="h-full bg-blue-500 dark:bg-blue-400 rounded-full transition-all duration-500"
                style={{ width: `${metrics?.cpu_percent ?? 0}%` }}
              />
            </div>
          </div>
        </Card>

        <Card className="p-4 shadow-sm border-border/50">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-muted-foreground">RAM</div>
              <div className="text-2xl font-bold text-foreground">
                {metrics?.memory?.percent ?? 0}%
              </div>
            </div>
            <div className="w-24 h-6 bg-secondary rounded-full overflow-hidden">
              <div
                className="h-full bg-emerald-500 dark:bg-emerald-400 rounded-full transition-all duration-500"
                style={{ width: `${metrics?.memory?.percent ?? 0}%` }}
              />
            </div>
          </div>
        </Card>

        <Card className="p-4 shadow-sm border-border/50">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-muted-foreground">Disk</div>
              <div className="text-2xl font-bold text-foreground">
                {metrics?.disk?.percent ?? 0}%
              </div>
            </div>
            <div className="w-24 h-6 bg-secondary rounded-full overflow-hidden">
              <div
                className="h-full bg-amber-500 dark:bg-amber-400 rounded-full transition-all duration-500"
                style={{ width: `${metrics?.disk?.percent ?? 0}%` }}
              />
            </div>
          </div>
        </Card>
      </div>

      {/* chart */}
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
              <Line
                type="monotone"
                dataKey="cpu"
                name="CPU %"
                stroke="#3B82F6"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
              <Line
                type="monotone"
                dataKey="mem"
                name="RAM %"
                stroke="#10B981"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
              <Line
                type="monotone"
                dataKey="disk"
                name="Disk %"
                stroke="#F59E0B"
                strokeWidth={2}
                dot={false}
                isAnimationActive={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* no separate detail cards: metrics are shown in the combined chart above */}

      {metrics?.error && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardHeader>
            <CardTitle className="text-destructive">警告</CardTitle>
          </CardHeader>
          <CardContent>
            <p>{metrics.error}</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
