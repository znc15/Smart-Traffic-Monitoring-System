import { useState, useEffect, useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Skeleton } from "@/ui/skeleton";
import {
  Video,
  Car,
  Bike,
  AlertTriangle,
  CheckCircle,
  Clock,
  Wifi,
  WifiOff,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import VideoModal from "./VideoModal";
import { getThresholdForRoad } from "../../../../config/trafficThresholds";
import { normalizeDensityStatus, normalizeSpeedStatus } from "@/utils/normalize";

interface VehicleData {
  count_car: number;
  count_motor: number;
  speed_car: number;
  speed_motor: number;
  online?: boolean;
  density_status?: string;
  speed_status?: string;
}

interface FrameData {
  [roadName: string]: {
    frame: string; // Now contains blob URL
  };
}

interface TrafficData {
  [roadName: string]: VehicleData;
}

interface VideoMonitorProps {
  frameData: FrameData;
  trafficData: TrafficData;
  allowedRoads: string[];
  selectedRoad: string | null;
  setSelectedRoad: (road: string | null) => void;
  loading: boolean;
  isFullscreen: boolean;
}

const VideoMonitor = ({
  frameData,
  trafficData,
  allowedRoads,
  selectedRoad,
  setSelectedRoad,
  loading,
  isFullscreen,
}: VideoMonitorProps) => {
  const [modalOpen, setModalOpen] = useState(false);
  const [modalRoadName, setModalRoadName] = useState<string>("");
  const [currentPage, setCurrentPage] = useState(0);
  const [autoPaging, setAutoPaging] = useState(true);

  const perPage = isFullscreen ? 8 : 6;
  const totalPages = Math.max(1, Math.ceil(allowedRoads.length / perPage));
  const pagedRoads = allowedRoads.slice(currentPage * perPage, (currentPage + 1) * perPage);

  // 自动翻页
  useEffect(() => {
    if (!autoPaging || totalPages <= 1) return;
    const timer = setInterval(() => {
      setCurrentPage((p) => (p + 1) % totalPages);
    }, 10000);
    return () => clearInterval(timer);
  }, [autoPaging, totalPages]);

  // 页数越界修正
  useEffect(() => {
    if (currentPage >= totalPages) setCurrentPage(0);
  }, [allowedRoads.length, currentPage, totalPages]);

  const goPage = useCallback((page: number) => {
    setCurrentPage(page);
    setAutoPaging(false);
    setTimeout(() => setAutoPaging(true), 30000);
  }, []);

  const getTrafficStatus = (roadName: string) => {
    const data = trafficData[roadName];
    if (!data)
      return {
        status: "unknown",
        color: "gray",
        icon: Clock,
        text: "未知",
      };

    if (data.online === false)
      return {
        status: "offline",
        color: "gray",
        icon: WifiOff,
        text: "离线",
      };

    // Prefer backend-provided classification when available
    const density = normalizeDensityStatus(data.density_status);
    if (density === "congested") {
      return { status: "congested", color: "red", icon: AlertTriangle, text: "拥堵" };
    }
    if (density === "busy") {
      return { status: "busy", color: "yellow", icon: Clock, text: "较拥挤" };
    }
    if (density === "clear") {
      return { status: "clear", color: "green", icon: CheckCircle, text: "畅通" };
    }
    if (density === "offline") {
      return { status: "offline", color: "gray", icon: WifiOff, text: "离线" };
    }

    // Fallback to previous local thresholds if backend not providing
    const threshold = getThresholdForRoad(roadName);
    const totalVehicles = data.count_car + data.count_motor;

    if (totalVehicles > threshold.c2)
      return {
        status: "congested",
        color: "red",
        icon: AlertTriangle,
        text: "拥堵",
      };
    if (totalVehicles > threshold.c1)
      return {
        status: "busy",
        color: "yellow",
        icon: Clock,
        text: "较拥挤",
      };
    return {
      status: "clear",
      color: "green",
      icon: CheckCircle,
      text: "畅通",
    };
  };

  const getSpeedStatus = (roadName: string) => {
    const data = trafficData[roadName];
    if (!data) return { speedText: "未知", speedColor: "gray" };

    const speed = normalizeSpeedStatus(data.speed_status);
    if (speed === "fast") return { speedText: "较快", speedColor: "green" };
    if (speed === "slow") return { speedText: "较慢", speedColor: "orange" };
    if (speed === "unknown") return { speedText: "未知", speedColor: "gray" };

    // Fallback
    const threshold = getThresholdForRoad(roadName);
    const avgSpeed = (data.speed_car + data.speed_motor) / 2;

    if (avgSpeed >= threshold.v)
      return { speedText: "较快", speedColor: "green" };
    return { speedText: "较慢", speedColor: "orange" };
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Video className="h-4 w-4 sm:h-5 sm:w-5" />
            <span className="text-sm sm:text-base">视频监控</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div
            className={`grid gap-3 sm:gap-4 ${
              isFullscreen
                ? "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
                : "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
            }`}
          >
            {allowedRoads.map((road) => (
              <div key={road} className="space-y-2 sm:space-y-3">
                <Skeleton className="aspect-[3/2] w-full max-w-sm mx-auto rounded-lg" />
                <Skeleton className="h-3 sm:h-4 w-3/4" />
                <Skeleton className="h-3 sm:h-4 w-1/2" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="h-full">
      <CardHeader className="px-3 sm:px-6">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <CardTitle className="flex items-center space-x-2">
            <Video className="h-4 w-4 sm:h-5 sm:w-5" />
            <span className="text-sm sm:text-base">交通摄像头</span>
          </CardTitle>
          {/* Connection Status */}
          {Object.keys(frameData).length > 0 && (
            <div
              className={`flex items-center gap-2 px-2 sm:px-3 py-1 sm:py-1.5 rounded-lg text-xs sm:text-sm font-medium ${
                Object.keys(trafficData).length > 0
                  ? "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400"
                  : "bg-destructive/10 text-destructive"
              }`}
            >
              {Object.keys(trafficData).length > 0 ? (
                <>
                  <Wifi className="h-3 w-3 sm:h-4 sm:w-4" />
                  <span>
                    已连接 {Object.keys(trafficData).length}/{allowedRoads.length} 路摄像头
                  </span>
                </>
              ) : (
                <>
                  <WifiOff className="h-3 w-3 sm:h-4 sm:w-4" />
                  <span>正在连接...</span>
                </>
              )}
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent className="px-3 sm:px-6 max-h-[calc(100vh-12rem)] overflow-y-auto overscroll-contain">
        <div
          className={`grid gap-4 sm:gap-6 ${
            isFullscreen
              ? "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
              : "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3"
          }`}
        >
          <AnimatePresence mode="wait">
            {pagedRoads.map((roadName) => {
              const frame = frameData[roadName];
              const data = trafficData[roadName];
              const { color, text } = getTrafficStatus(roadName);
              const { speedText, speedColor } = getSpeedStatus(roadName);
              const isSelected = selectedRoad === roadName;
              const isOffline = data?.online === false;

              return (
                <motion.div
                  key={roadName}
                  initial={{ opacity: 0 }}
                  animate={{
                    opacity: 1,
                    transition: { duration: 0.2 },
                  }}
                  exit={{ opacity: 0 }}
                  className={`relative overflow-hidden rounded-xl border transition-all duration-200 cursor-pointer w-full ${
                    isSelected
                      ? "border-primary shadow-sm"
                      : "border-border/40 hover:border-border hover:shadow-sm"
                  } ${isOffline ? "opacity-70" : ""}`}
                  onClick={() => {
                    setModalRoadName(roadName);
                    setModalOpen(true);
                    if (setSelectedRoad) setSelectedRoad(roadName);
                  }}
                >
                  {/* Video Frame (responsive) */}
                  <div className="relative w-full aspect-[3/2] bg-muted overflow-hidden">
                    {frame?.frame ? (
                      <img
                        src={frame.frame}
                        alt={`摄像头 ${roadName}`}
                        className="w-full h-full object-cover block"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <Video className="h-8 w-8 sm:h-12 sm:w-12 text-muted-foreground/40" />
                      </div>
                    )}

                    {/* Offline overlay */}
                    {isOffline && (
                      <div className="absolute inset-0 bg-black/50 flex flex-col items-center justify-center z-10">
                        <WifiOff className="h-8 w-8 text-white/80 mb-1" />
                        <span className="text-white/80 text-xs font-medium">节点离线</span>
                      </div>
                    )}

                    {/* Click to expand hint */}
                    <div className="absolute inset-0 bg-black/0 hover:bg-black/10 transition-colors duration-200 flex items-center justify-center opacity-0 hover:opacity-100">
                      <div className="bg-card/90 text-foreground px-2 py-1 sm:px-3 sm:py-2 rounded-lg text-xs sm:text-sm font-medium backdrop-blur-sm">
                        点击放大
                      </div>
                    </div>
                  </div>

                  {/* Info Panel */}
                  <div className="bg-card p-2 sm:p-3">
                    <div className="flex items-center justify-between mb-1.5 sm:mb-2">
                      <h3 className="font-medium text-sm sm:text-base truncate">{roadName}</h3>
                      <div className="flex items-center gap-1.5 shrink-0 ml-2">
                        <span className={`inline-block h-2 w-2 rounded-full ${
                          color === "red" ? "bg-red-500" : color === "yellow" ? "bg-amber-400" : color === "green" ? "bg-emerald-500" : "bg-gray-400"
                        }`} />
                        <span className="text-xs text-muted-foreground">{text}</span>
                        {data && (
                          <>
                            <span className="text-muted-foreground/40">·</span>
                            <span
                              className={`text-xs ${
                                speedColor === "green"
                                  ? "text-emerald-600 dark:text-emerald-400"
                                  : speedColor === "gray"
                                  ? "text-muted-foreground"
                                  : "text-amber-600 dark:text-amber-400"
                              }`}
                            >
                              {speedText}
                            </span>
                          </>
                        )}
                      </div>
                    </div>

                    {data ? (
                      <div className="flex items-center gap-3 sm:gap-4 text-xs text-muted-foreground">
                        <span className="inline-flex items-center gap-1">
                          <Car className="h-3 w-3" />
                          <span className="font-medium text-foreground">{data.count_car}</span>
                          <span className="text-muted-foreground/60">{data.speed_car.toFixed(0)}km/h</span>
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Bike className="h-3 w-3" />
                          <span className="font-medium text-foreground">{data.count_motor}</span>
                          <span className="text-muted-foreground/60">{data.speed_motor.toFixed(0)}km/h</span>
                        </span>
                      </div>
                    ) : (
                      <p className="text-xs text-muted-foreground/50">正在加载数据...</p>
                    )}
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>

        {/* 分页控件 */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-3">
            <button onClick={() => goPage((currentPage - 1 + totalPages) % totalPages)} className="p-1 rounded-md hover:bg-muted text-muted-foreground">
              <ChevronLeft className="h-4 w-4" />
            </button>
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                onClick={() => goPage(i)}
                className={`h-2 rounded-full transition-all ${i === currentPage ? "w-6 bg-primary" : "w-2 bg-muted-foreground/30 hover:bg-muted-foreground/50"}`}
              />
            ))}
            <button onClick={() => goPage((currentPage + 1) % totalPages)} className="p-1 rounded-md hover:bg-muted text-muted-foreground">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        )}
      </CardContent>

      {/* Video Modal */}
      <VideoModal
        isOpen={modalOpen && !!modalRoadName}
        onClose={() => {
          setModalOpen(false);
          setModalRoadName("");
        }}
        roadName={modalRoadName}
        frameData={
          modalRoadName ? frameData[modalRoadName]?.frame || null : null
        }
        trafficData={modalRoadName ? trafficData[modalRoadName] : undefined}
      />
    </Card>
  );
};

export default VideoMonitor;
