import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Badge } from "@/ui/badge";
import { Skeleton } from "@/ui/skeleton";
import {
  MapPin,
  Car,
  Bike,
  AlertTriangle,
  CheckCircle,
  Clock,
  Gauge,
} from "lucide-react";
import VideoMonitor from "../../video/components/VideoMonitor";
import { motion, AnimatePresence } from "framer-motion";
import {
  useMultipleTrafficInfo,
  useMultipleFrameStreams,
} from "../../../../hooks/useWebSocket";
import { endpoints } from "../../../../config";
import { getThresholdForRoad } from "../../../../config/trafficThresholds";

// Import types from the WebSocket hook
type VehicleData = {
  count_car: number;
  count_motor: number;
  speed_car: number;
  speed_motor: number;
};

type TrafficBackendData = VehicleData & {
  density_status?: string;
  speed_status?: string;
};

const TrafficDashboard = () => {
  const [selectedRoad, setSelectedRoad] = useState<string | null>(null);
  const [localFullscreen] = useState(false);

  const [allowedRoads, setAllowedRoads] = useState<string[]>([]);

  useEffect(() => {
    const fetchRoads = async () => {
      try {
        // roads_name endpoint không cần authentication
        const res = await fetch(endpoints.roadNames);
        if (!res.ok) {
          console.error("Failed to fetch road names");
          setAllowedRoads([
            "Văn Phú",
            "Nguyễn Trãi",
            "Ngã Tư Sở",
            "Đường Láng",
          ]);
          return;
        }
        const json = await res.json();
        const names: string[] = json?.road_names ?? [];
        setAllowedRoads(names);
      } catch (error) {
        console.error("Error fetching roads:", error);
        setAllowedRoads([
          "Văn Phú",
          "Nguyễn Trãi",
          "Ngã Tư Sở",
          "Đường Láng",
          "Văn Quán",
        ]);
      }
    };
    fetchRoads();
  }, []);

  // Use WebSocket for traffic data
  const { trafficData, isAnyConnected } = useMultipleTrafficInfo(allowedRoads);
  const { frameData: frames } = useMultipleFrameStreams(allowedRoads);

  const loading = !isAnyConnected;

  const getTrafficStatus = (roadName: string) => {
    const data = trafficData[roadName] as VehicleData | undefined;
    if (!data) return { status: "unknown", color: "gray", icon: Clock };
    // Prefer backend-provided classification when available
    const densityFromBackend = (data as TrafficBackendData).density_status;
    if (densityFromBackend) {
      if (densityFromBackend === "Tắc nghẽn")
        return { status: "congested", color: "red", icon: AlertTriangle };
      if (densityFromBackend === "Đông đúc")
        return { status: "busy", color: "yellow", icon: Clock };
      if (densityFromBackend === "Thông thoáng")
        return { status: "clear", color: "green", icon: CheckCircle };
    }
    // Fallback: compute from local thresholds when backend doesn't provide classification
    const threshold = getThresholdForRoad(roadName);
    const totalVehicles = (data.count_car ?? 0) + (data.count_motor ?? 0);
    if (totalVehicles > threshold.c2)
      return { status: "congested", color: "red", icon: AlertTriangle };
    if (totalVehicles > threshold.c1)
      return { status: "busy", color: "yellow", icon: Clock };
    return { status: "clear", color: "green", icon: CheckCircle };
  };

  const getSpeedStatus = (roadName: string) => {
    const data = trafficData[roadName] as VehicleData | undefined;
    if (!data) return { speedText: "未知", speedColor: "gray" };
    const speedFromBackend = (data as TrafficBackendData).speed_status;
    if (speedFromBackend) {
      if (speedFromBackend === "Nhanh chóng")
        return { speedText: "较快", speedColor: "green" };
      if (speedFromBackend === "Chậm chạp")
        return { speedText: "较慢", speedColor: "orange" };
    }
    // Fallback: compute from local thresholds
    const threshold = getThresholdForRoad(roadName);
    const avgSpeed = ((data.speed_car ?? 0) + (data.speed_motor ?? 0)) / 2;
    if (avgSpeed >= threshold.v)
      return { speedText: "较快", speedColor: "green" };
    return { speedText: "较慢", speedColor: "orange" };
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "congested":
        return "拥堵";
      case "busy":
        return "较拥挤";
      case "clear":
        return "畅通";
      default:
        return "未知";
    }
  };

  return (
    <div className="min-h-screen pt-4 px-2 sm:px-4 space-y-4 sm:space-y-6">
      {/* Connection Status Banner - REMOVED, now inside VideoMonitor */}

      {/* Main Content */}
      <div className="space-y-4 sm:space-y-6">
        <div
          className={`grid gap-4 sm:gap-6 ${
            localFullscreen ? "grid-cols-1" : "grid-cols-1 lg:grid-cols-4"
          }`}
        >
          {/* Video Monitoring */}
          <div className={localFullscreen ? "col-span-1" : "col-span-3"}>
            <VideoMonitor
              frameData={frames}
              trafficData={trafficData}
              allowedRoads={allowedRoads}
              selectedRoad={selectedRoad}
              setSelectedRoad={setSelectedRoad}
              loading={loading}
              isFullscreen={localFullscreen}
            />
          </div>

          {/* Traffic Status Cards */}
          {!localFullscreen && (
            <div className="space-y-4 w-full lg:max-w-xs lg:justify-self-end">
              <Card className="shadow-lg border-border/50 bg-card">
                <CardHeader className="py-2 bg-transparent border-b border-border">
                  <CardTitle className="flex items-center space-x-2 text-base text-foreground">
                    <MapPin className="h-5 w-5 text-primary" />
                    <span>交通状态</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3 px-4 max-h-60 overflow-y-auto overscroll-contain">
                  {loading ? (
                    // Loading skeleton
                    <>
                      {[1, 2, 3, 4, 5].map((i) => (
                        <div
                          key={i}
                          className="flex items-center justify-between p-3 rounded-lg bg-muted/50"
                        >
                          <Skeleton className="h-5 w-32" />
                          <Skeleton className="h-6 w-20" />
                        </div>
                      ))}
                    </>
                  ) : allowedRoads.length === 0 ? (
                    // Empty state
                    <div className="text-center py-8">
                      <Clock className="h-12 w-12 text-muted-foreground/40 mx-auto mb-3" />
                      <p className="text-muted-foreground text-sm">
                        暂无道路
                      </p>
                    </div>
                  ) : (
                    <AnimatePresence>
                      {allowedRoads.map((road) => {
                        const { status, color } = getTrafficStatus(road);
                        const { speedText, speedColor } = getSpeedStatus(road);
                        const data = trafficData[road];

                        return (
                          <motion.div
                            key={road}
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.3 }}
                            className="flex flex-col p-3 rounded-lg bg-card border border-border/50 hover:bg-accent/50 hover:border-border transition-all cursor-pointer hover:shadow-md space-y-2"
                            onClick={() => setSelectedRoad(road)}
                          >
                            {/* Tên đường và nhãn mật độ */}
                            <div className="flex items-center justify-between">
                              <span className="font-semibold text-sm text-foreground">
                                {road}
                              </span>
                              <Badge
                                variant={
                                  color === "red"
                                    ? "destructive"
                                    : color === "yellow"
                                    ? "secondary"
                                    : "default"
                                }
                                className="text-xs h-5 leading-none px-2 py-0"
                              >
                                {getStatusText(status)}
                              </Badge>
                            </div>

                            {/* Thông tin số lượng và tốc độ */}
                            <div className="flex items-center justify-between gap-2">
                              {data && (
                                <div className="text-xs font-medium text-muted-foreground flex items-center space-x-1">
                                  <Car className="h-3 w-3 text-primary" />
                                  <span>{String(data.count_car)}</span>
                                  <Bike className="h-3 w-3 ml-2 text-emerald-600 dark:text-emerald-400" />
                                  <span>{String(data.count_motor)}</span>
                                </div>
                              )}
                              <Badge
                                variant="outline"
                                className={`flex items-center space-x-1 text-xs px-2 py-0 h-5 leading-none ${
                                  speedColor === "green"
                                    ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20"
                                    : "bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20"
                                }`}
                              >
                                <Gauge className="h-3 w-3" />
                                <span className="font-medium">{speedText}</span>
                              </Badge>
                            </div>
                          </motion.div>
                        );
                      })}
                    </AnimatePresence>
                  )}
                </CardContent>
              </Card>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TrafficDashboard;
