import { useState } from "react";
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
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import VideoModal from "./VideoModal";
import { getThresholdForRoad } from "../../../../config/trafficThresholds";

interface VehicleData {
  count_car: number;
  count_motor: number;
  speed_car: number;
  speed_motor: number;
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

  const getTrafficStatus = (roadName: string) => {
    const data = trafficData[roadName];
    if (!data)
      return {
        status: "unknown",
        color: "gray",
        icon: Clock,
        text: "未知",
      };

    // Prefer backend-provided classification when available
    const densityFromBackend = data.density_status;
    if (densityFromBackend) {
      if (densityFromBackend === "Tắc nghẽn")
        return {
          status: "congested",
          color: "red",
          icon: AlertTriangle,
          text: "拥堵",
        };
      if (densityFromBackend === "Đông đúc")
        return {
          status: "busy",
          color: "yellow",
          icon: Clock,
          text: "较拥挤",
        };
      if (densityFromBackend === "Thông thoáng")
        return {
          status: "clear",
          color: "green",
          icon: CheckCircle,
          text: "畅通",
        };
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

    const speedFromBackend = data.speed_status;
    if (speedFromBackend) {
      if (speedFromBackend === "Nhanh chóng")
        return { speedText: "较快", speedColor: "green" };
      if (speedFromBackend === "Chậm chạp")
        return { speedText: "较慢", speedColor: "orange" };
    }

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
          <AnimatePresence>
            {allowedRoads.map((roadName) => {
              const frame = frameData[roadName];
              const data = trafficData[roadName];
              const { color, text } = getTrafficStatus(roadName);
              const { speedText, speedColor } = getSpeedStatus(roadName);
              const isSelected = selectedRoad === roadName;

              return (
                <motion.div
                  key={roadName}
                  initial={{ opacity: 0 }}
                  animate={{
                    opacity: 1,
                    transition: { duration: 0.2 },
                  }}
                  exit={{ opacity: 0 }}
                  className={`relative overflow-hidden rounded-xl border transition-all duration-200 cursor-pointer inline-block w-full max-w-sm mx-auto ${
                    isSelected
                      ? "border-primary shadow-sm"
                      : "border-border/40 hover:border-border hover:shadow-sm"
                  }`}
                  onClick={() => {
                    setModalRoadName(roadName);
                    setModalOpen(true);
                    if (setSelectedRoad) setSelectedRoad(roadName);
                  }}
                >
                  {/* Video Frame (responsive) */}
                  <div className="relative w-full max-w-sm mx-auto aspect-[3/2] bg-muted overflow-hidden">
                    {frame?.frame ? (
                      <img
                        src={frame.frame}
                        alt={`摄像头 ${roadName}`}
                        className="w-full h-full object-contain block"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <Video className="h-8 w-8 sm:h-12 sm:w-12 text-muted-foreground/40" />
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
                            <span className={`text-xs ${speedColor === "green" ? "text-emerald-600 dark:text-emerald-400" : "text-amber-600 dark:text-amber-400"}`}>{speedText}</span>
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
