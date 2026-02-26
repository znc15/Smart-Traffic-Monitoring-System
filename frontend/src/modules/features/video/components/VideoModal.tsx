import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Maximize2, Minimize2, Video, Car, Bike, Gauge, Activity } from "lucide-react";
import { Button } from "@/ui/button";
import { getThresholdForRoad } from "../../../../config/trafficThresholds";
import { normalizeDensityStatus, normalizeSpeedStatus } from "@/utils/normalize";

interface VideoModalProps {
  isOpen: boolean;
  onClose: () => void;
  roadName: string;
  frameData: string | null;
  trafficData?: {
    count_car: number;
    count_motor: number;
    speed_car: number;
    speed_motor: number;
    density_status?: string;
    speed_status?: string;
  };
}

const VideoModal = ({
  isOpen,
  onClose,
  roadName,
  frameData,
  trafficData,
}: VideoModalProps) => {
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (isOpen) {
      document.addEventListener("keydown", handleEscape);
      document.body.style.overflow = "hidden";
    }
    return () => {
      document.removeEventListener("keydown", handleEscape);
      document.body.style.overflow = "unset";
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  // 计算密度状态
  const getDensity = () => {
    const density = normalizeDensityStatus(trafficData?.density_status);
    if (density === "congested") return { text: "拥堵", cls: "bg-red-500/10 text-red-600 dark:text-red-400" };
    if (density === "busy") return { text: "较拥挤", cls: "bg-amber-500/10 text-amber-600 dark:text-amber-400" };
    if (density === "clear") return { text: "畅通", cls: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" };
    if (density === "offline") return { text: "离线", cls: "bg-muted text-muted-foreground" };
    if (!trafficData) return { text: "未知", cls: "bg-muted text-muted-foreground" };
    const threshold = getThresholdForRoad(roadName);
    const total = (trafficData.count_car ?? 0) + (trafficData.count_motor ?? 0);
    if (total > threshold.c2) return { text: "拥堵", cls: "bg-red-500/10 text-red-600 dark:text-red-400" };
    if (total > threshold.c1) return { text: "较拥挤", cls: "bg-amber-500/10 text-amber-600 dark:text-amber-400" };
    return { text: "畅通", cls: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" };
  };

  // 计算速度状态
  const getSpeed = () => {
    const speed = normalizeSpeedStatus(trafficData?.speed_status);
    if (speed === "fast") return { text: "较快", cls: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" };
    if (speed === "slow") return { text: "较慢", cls: "bg-amber-500/10 text-amber-600 dark:text-amber-400" };
    if (speed === "unknown") return { text: "未知", cls: "bg-muted text-muted-foreground" };
    if (!trafficData) return { text: "未知", cls: "bg-muted text-muted-foreground" };
    const threshold = getThresholdForRoad(roadName);
    const avg = ((trafficData.speed_car ?? 0) + (trafficData.speed_motor ?? 0)) / 2;
    if (avg >= threshold.v) return { text: "较快", cls: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400" };
    return { text: "较慢", cls: "bg-amber-500/10 text-amber-600 dark:text-amber-400" };
  };

  const density = getDensity();
  const speed = getSpeed();

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.9, opacity: 0 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          className={`relative bg-card rounded-2xl shadow-2xl overflow-hidden flex flex-col ${
            isFullscreen
              ? "w-screen h-screen rounded-none"
              : "w-[95vw] max-w-5xl max-h-[90vh]"
          }`}
          onClick={(e) => e.stopPropagation()}
        >
          {/* 顶栏 */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-muted/30">
            <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
              <Video className="h-5 w-5 text-primary" />
              {roadName}
            </h2>
            <div className="flex items-center gap-1">
              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setIsFullscreen(!isFullscreen)}>
                {isFullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
              </Button>
              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
                <X className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* 主体 */}
          <div className="flex flex-col lg:flex-row flex-1 overflow-hidden">
            {/* 视频区域 */}
            <div className="flex-1 bg-black flex items-center justify-center min-h-[30vh]">
              {frameData ? (
                <img
                  src={frameData}
                  alt={roadName}
                  className="w-full h-full object-contain"
                />
              ) : (
                <div className="text-center text-muted-foreground">
                  <div className="w-10 h-10 border-3 border-muted-foreground/30 border-t-muted-foreground rounded-full animate-spin mx-auto mb-3" />
                  <p className="text-sm">正在加载...</p>
                </div>
              )}
            </div>

            {/* 信息面板 */}
            {trafficData && (
              <div className="lg:w-72 border-t lg:border-t-0 lg:border-l border-border bg-muted/20 p-4 overflow-y-auto space-y-4">
                {/* 总览状态 */}
                <div className="grid grid-cols-2 gap-2">
                  <div className="rounded-lg bg-card border border-border/50 p-3 text-center">
                    <Activity className="h-4 w-4 text-muted-foreground mx-auto mb-1.5" />
                    <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${density.cls}`}>
                      {density.text}
                    </span>
                    <p className="text-[10px] text-muted-foreground mt-1">密度</p>
                  </div>
                  <div className="rounded-lg bg-card border border-border/50 p-3 text-center">
                    <Gauge className="h-4 w-4 text-muted-foreground mx-auto mb-1.5" />
                    <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${speed.cls}`}>
                      {speed.text}
                    </span>
                    <p className="text-[10px] text-muted-foreground mt-1">速度</p>
                  </div>
                </div>

                {/* 汽车 */}
                <div className="rounded-lg bg-card border border-border/50 p-3">
                  <h4 className="text-xs font-medium text-foreground flex items-center gap-1.5 mb-2.5">
                    <Car className="h-3.5 w-3.5 text-primary" />
                    汽车
                  </h4>
                  <div className="grid grid-cols-2 gap-2">
                    <div className="bg-muted/50 rounded-md px-2.5 py-2 text-center">
                      <p className="text-lg font-semibold text-foreground">{trafficData.count_car ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">数量</p>
                    </div>
                    <div className="bg-muted/50 rounded-md px-2.5 py-2 text-center">
                      <p className="text-lg font-semibold text-foreground">{trafficData.speed_car ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">km/h</p>
                    </div>
                  </div>
                </div>

                {/* 摩托车 */}
                <div className="rounded-lg bg-card border border-border/50 p-3">
                  <h4 className="text-xs font-medium text-foreground flex items-center gap-1.5 mb-2.5">
                    <Bike className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />
                    摩托车
                  </h4>
                  <div className="grid grid-cols-2 gap-2">
                    <div className="bg-muted/50 rounded-md px-2.5 py-2 text-center">
                      <p className="text-lg font-semibold text-foreground">{trafficData.count_motor ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">数量</p>
                    </div>
                    <div className="bg-muted/50 rounded-md px-2.5 py-2 text-center">
                      <p className="text-lg font-semibold text-foreground">{trafficData.speed_motor ?? 0}</p>
                      <p className="text-[10px] text-muted-foreground">km/h</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default VideoModal;
