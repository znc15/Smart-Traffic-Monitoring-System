import React, { useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/ui/tabs";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";
import {
  BarChart3,
  PieChart as PieChartIcon,
  LineChart as LineChartIcon,
  AlertCircle,
} from "lucide-react";

type VehicleData = {
  count_car: number;
  count_motor: number;
  speed_car: number;
  speed_motor: number;
};
type TrafficData = Record<string, VehicleData>;
export type HistoricalData = { time: string; [key: string]: string | number };

interface Props {
  trafficData: TrafficData;
  allowedRoads: string[];
  historicalData?: HistoricalData[]; // optional external history from store
}

const INTERNAL_MAX = 60; // fallback cap for component-local history

const TrafficAnalytics: React.FC<Props> = ({
  trafficData,
  allowedRoads,
  historicalData,
}) => {
  // internal fallback history used only when `historicalData` prop isn't provided
  const [internalHistory, setInternalHistory] = useState<HistoricalData[]>([]);

  useEffect(() => {
    if (historicalData && Array.isArray(historicalData)) return; // store provides history
    if (Object.keys(trafficData).length === 0) return;

    const now = new Date();
    const timeString = now.toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });

    const newPoint: HistoricalData = {
      time: timeString,
      ...Object.entries(trafficData).reduce((acc, [road, d]) => {
        acc[`${road}_cars`] = d.count_car;
        acc[`${road}_motors`] = d.count_motor;
        acc[`${road}_car_speed`] = d.speed_car;
        acc[`${road}_motor_speed`] = d.speed_motor;
        acc[`${road}_total`] = d.count_car + d.count_motor;
        return acc;
      }, {} as Record<string, number>),
    };

    setInternalHistory((prev) => {
      // avoid duplicate consecutive points
      const last = prev[prev.length - 1];
      if (last && JSON.stringify(last) === JSON.stringify(newPoint))
        return prev;
      const next = [...prev, newPoint].slice(-INTERNAL_MAX);
      return next;
    });
  }, [trafficData, historicalData]);

  const trendsData =
    historicalData && Array.isArray(historicalData)
      ? historicalData
      : internalHistory;

  const vehicleCountData = useMemo(
    () =>
      allowedRoads.map((road) => {
        const d = trafficData[road];
        return {
          road: road.length > 12 ? `${road.slice(0, 12)}...` : road,
          fullRoad: road,
          cars: d?.count_car || 0,
          motors: d?.count_motor || 0,
          total: (d?.count_car || 0) + (d?.count_motor || 0),
        };
      }),
    [allowedRoads, trafficData]
  );

  const speedData = useMemo(
    () =>
      allowedRoads.map((road) => {
        const d = trafficData[road];
        return {
          road: road.length > 12 ? `${road.slice(0, 12)}...` : road,
          fullRoad: road,
          carSpeed: d?.speed_car || 0,
          motorSpeed: d?.speed_motor || 0,
        };
      }),
    [allowedRoads, trafficData]
  );

  const pieData = useMemo(
    () =>
      allowedRoads
        .map((road) => {
          const d = trafficData[road];
          const total = (d?.count_car || 0) + (d?.count_motor || 0);
          return {
            name: road,
            value: total,
            cars: d?.count_car || 0,
            motors: d?.count_motor || 0,
          };
        })
        .filter((i) => i.value > 0),
    [allowedRoads, trafficData]
  );

  const COLORS = ["#4F6BF0", "#0EA5E9", "#7C3AED", "#06B6D4", "#60A5FA"];

  const tooltipStyle: React.CSSProperties = {
    backgroundColor: "var(--card)",
    border: "1px solid var(--border)",
    borderRadius: 10,
    color: "var(--foreground)",
    boxShadow: "0 4px 24px oklch(0 0 0 / 8%)",
  };

  const EmptyState: React.FC<{ message: string }> = ({ message }) => (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <AlertCircle className="h-16 w-16 text-muted-foreground/40 mb-4" />
      <p className="text-muted-foreground text-lg font-medium">
        {message}
      </p>
      <p className="text-muted-foreground/60 text-sm mt-2">
        系统开始采集数据后将显示在此处
      </p>
    </div>
  );

  const hasData = Object.keys(trafficData).length > 0;

  return (
    <div className="space-y-6">
      <Tabs defaultValue="overview" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3 max-w-md mx-auto">
          <TabsTrigger
            value="overview"
            className="flex items-center space-x-2 text-xs sm:text-sm"
          >
            <BarChart3 className="h-4 w-4" />
            <span className="hidden sm:inline">概览</span>
            <span className="sm:hidden">概览</span>
          </TabsTrigger>
          <TabsTrigger
            value="trends"
            className="flex items-center space-x-2 text-xs sm:text-sm"
          >
            <LineChartIcon className="h-4 w-4" />
            <span className="hidden sm:inline">趋势</span>
            <span className="sm:hidden">趋势</span>
          </TabsTrigger>
          <TabsTrigger
            value="distribution"
            className="flex items-center space-x-2 text-xs sm:text-sm"
          >
            <PieChartIcon className="h-4 w-4" />
            <span className="hidden sm:inline">分布</span>
            <span className="sm:hidden">分布</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4 sm:space-y-6">
          {!hasData ? (
            <Card className="shadow-lg">
              <CardContent className="pt-6">
                <EmptyState message="暂无交通数据" />
              </CardContent>
            </Card>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
              <Card className="shadow-lg">
                <CardHeader className="pb-4">
                  <CardTitle className="text-base sm:text-lg">
                    各路段车辆数量
                  </CardTitle>
                </CardHeader>
                <CardContent className="flex justify-center px-2 sm:px-4">
                  <ResponsiveContainer width="100%" height={350}>
                    <BarChart data={vehicleCountData}>
                      <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                      <XAxis
                        dataKey="road"
                        tick={{ fontSize: 12 }}
                        angle={-15}
                        textAnchor="end"
                        height={60}
                      />
                      <YAxis tick={{ fontSize: 12 }} />
                      <Tooltip
                        formatter={(value, name) => [
                          value,
                          name === "cars" ? "汽车" : "摩托车",
                        ]}
                        labelFormatter={(label) =>
                          vehicleCountData.find((d) => d.road === label)
                            ?.fullRoad || label
                        }
                        contentStyle={tooltipStyle}
                      />
                      <Bar
                        dataKey="cars"
                        fill="#4F6BF0"
                        name="cars"
                        radius={[4, 4, 0, 0]}
                      />
                      <Bar
                        dataKey="motors"
                        fill="#0EA5E9"
                        name="motors"
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>

              <Card className="shadow-lg">
                <CardHeader className="pb-4">
                  <CardTitle className="text-base sm:text-lg">
                    平均速度（km/h）
                  </CardTitle>
                </CardHeader>
                <CardContent className="flex justify-center px-2 sm:px-4">
                  <ResponsiveContainer width="100%" height={350}>
                    <BarChart data={speedData}>
                      <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                      <XAxis
                        dataKey="road"
                        tick={{ fontSize: 12 }}
                        angle={-15}
                        textAnchor="end"
                        height={60}
                      />
                      <YAxis tick={{ fontSize: 12 }} />
                      <Tooltip
                        formatter={(value, name) => [
                          `${Number(value).toFixed(1)} km/h`,
                          name === "carSpeed" ? "汽车" : "摩托车",
                        ]}
                        labelFormatter={(label) =>
                          speedData.find((d) => d.road === label)?.fullRoad ||
                          label
                        }
                        contentStyle={tooltipStyle}
                      />
                      <Bar
                        dataKey="carSpeed"
                        fill="#4F6BF0"
                        name="carSpeed"
                        radius={[4, 4, 0, 0]}
                      />
                      <Bar
                        dataKey="motorSpeed"
                        fill="#0EA5E9"
                        name="motorSpeed"
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </CardContent>
              </Card>
            </div>
          )}
        </TabsContent>

        <TabsContent value="trends">
          <Card className="shadow-lg">
            <CardHeader className="pb-4">
              <CardTitle className="text-base sm:text-lg">
                交通趋势（随时间）
              </CardTitle>
            </CardHeader>
            <CardContent className="px-2 sm:px-4">
              {trendsData.length === 0 ? (
                <EmptyState message="暂无历史数据" />
              ) : (
                <ResponsiveContainer width="100%" height={400}>
                  <LineChart data={trendsData}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                    <XAxis dataKey="time" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip contentStyle={tooltipStyle} />
                    <Legend wrapperStyle={{ fontSize: 12 }} />
                    {allowedRoads.map((road, index) => (
                      <Line
                        key={road}
                        type="monotone"
                        dataKey={`${road}_total`}
                        stroke={COLORS[index % COLORS.length]}
                        name={road}
                        strokeWidth={2}
                        dot={{ r: 3 }}
                        activeDot={{ r: 5 }}
                      />
                    ))}
                  </LineChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="distribution">
          <Card className="shadow-lg">
            <CardHeader className="pb-4">
              <CardTitle className="text-base sm:text-lg">
                车辆分布（按路段）
              </CardTitle>
            </CardHeader>
            <CardContent className="px-2 sm:px-4">
              {pieData.length === 0 ? (
                <EmptyState message="暂无分布数据" />
              ) : (
                <ResponsiveContainer width="100%" height={400}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) =>
                        `${name} (${(percent * 100).toFixed(0)}%)`
                      }
                      outerRadius={window.innerWidth < 640 ? 100 : 130}
                      fill="#4F6BF0"
                      dataKey="value"
                    >
                      {pieData.map((_, index) => (
                        <Cell
                          key={`cell-${index}`}
                          fill={COLORS[index % COLORS.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value, _, props) => [
                        `${value} 辆（${props.payload.cars} 汽车，${props.payload.motors} 摩托车）`,
                        "车辆总数",
                      ]}
                      contentStyle={tooltipStyle}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default TrafficAnalytics;
