import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Maximize2, Minimize2, Video, Car, Bike, Timer } from "lucide-react";
import { Button } from "@/ui/button";
import { getThresholdForRoad } from "../../../../config/trafficThresholds";

interface VideoModalProps {
  isOpen: boolean;
  onClose: () => void;
  roadName: string;
  frameData: string | null; // Now using blob URL string
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
      if (e.key === "Escape") {
        onClose();
      }
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

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm"
        onClick={onClose}
      >
        <motion.div
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          exit={{ scale: 0.8, opacity: 0 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          className={`relative bg-card rounded-xl shadow-2xl overflow-hidden ${
            isFullscreen
              ? "w-screen h-screen rounded-none"
              : "w-[95vw] sm:w-auto h-auto max-w-6xl mx-4"
          }`}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between p-3 sm:p-4 border-b border-border bg-muted/50">
            <h2 className="text-base sm:text-xl font-semibold text-foreground flex items-center gap-2">
              <Video className="h-5 w-5 sm:h-6 sm:w-6 text-primary" />
              <span className="truncate">摄像头：{roadName}</span>
            </h2>
            <div className="flex items-center space-x-2">
              <Button
                variant="ghost"
                size="icon"
                onClick={toggleFullscreen}
                className="text-muted-foreground hover:text-foreground"
              >
                {isFullscreen ? (
                  <Minimize2 className="h-5 w-5" />
                ) : (
                  <Maximize2 className="h-5 w-5" />
                )}
              </Button>
              <Button
                variant="ghost"
                size="icon"
                onClick={onClose}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
          </div>

          {/* Video Content */}
          <div className="flex flex-col lg:flex-row max-h-[85vh] sm:max-h-[80vh]">
            {/* Video */}
            <div className="relative bg-muted p-3 sm:p-6 flex-1 flex items-center justify-center min-h-[40vh] lg:min-h-0">
              <div className="relative w-full h-full flex items-center justify-center">
                {frameData ? (
                  <img
                    src={frameData}
                    alt={`摄像头 ${roadName}`}
                    className="max-w-full max-h-[50vh] sm:max-h-[60vh] lg:max-h-[70vh] object-contain rounded-lg shadow-lg"
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-white">
                    <div className="text-center">
                      <div className="w-12 h-12 sm:w-16 sm:h-16 border-4 border-white/30 border-t-white rounded-full animate-spin mx-auto mb-4"></div>
                      <p className="text-sm sm:text-base">正在加载视频...</p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Traffic Info - Responsive Side/Bottom Panel */}
            {trafficData && (
              <div className="p-3 sm:p-4 bg-muted/50 border-t lg:border-t-0 lg:border-l border-border overflow-y-auto w-full lg:w-80 max-h-[40vh] lg:max-h-[80vh]">
                <h3 className="text-sm sm:text-base font-semibold mb-3 sm:mb-4 text-foreground flex items-center space-x-2">
                  <Timer className="h-4 w-4 sm:h-5 sm:w-5 text-primary" />
                  <span>交通信息</span>
                </h3>

                {/* Traffic Status Section */}
                <div className="mb-3 sm:mb-4 bg-card p-2 sm:p-3 rounded-lg shadow-sm">
                  <h4 className="text-xs sm:text-sm font-medium mb-2 sm:mb-3 text-foreground flex items-center gap-2">
                    <Timer className="h-3 w-3 sm:h-4 sm:w-4 text-primary" />
                    状态
                  </h4>
                  <div className="space-y-2">
                    {/* Đánh giá về số lượng phương tiện */}
                    <div className="p-2 bg-muted/50 rounded">
                      {(() => {
                        // Prefer backend-provided label
                        const densityFromBackend = trafficData.density_status;
                        if (densityFromBackend) {
                          if (densityFromBackend === "Tắc nghẽn") {
                            return (
                              <div className="flex items-center justify-between">
                                <span className="text-xs text-muted-foreground">
                                  密度：
                                </span>
                                <span className="font-medium text-xs sm:text-sm bg-destructive/10 px-2 py-1 rounded text-destructive">
                                  拥堵
                                </span>
                              </div>
                            );
                          }
                          if (densityFromBackend === "Đông đúc") {
                            return (
                              <div className="flex items-center justify-between">
                                <span className="text-xs text-muted-foreground">
                                  密度：
                                </span>
                                <span className="font-medium text-xs sm:text-sm bg-amber-500/10 px-2 py-1 rounded text-amber-700 dark:text-amber-400">
                                  较拥挤
                                </span>
                              </div>
                            );
                          }
                          return (
                            <div className="flex items-center justify-between">
                              <span className="text-xs text-muted-foreground">
                                密度：
                              </span>
                              <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                                畅通
                              </span>
                            </div>
                          );
                        }

                        // Fallback: compute locally if backend not available
                        const threshold = getThresholdForRoad(roadName);
                        const totalVehicles =
                          (trafficData?.count_car || 0) +
                          (trafficData?.count_motor || 0);
                        if (totalVehicles > threshold.c2) {
                          return (
                            <div className="flex items-center justify-between">
                              <span className="text-xs text-muted-foreground">
                                密度：
                              </span>
                              <span className="font-medium text-xs sm:text-sm bg-destructive/10 px-2 py-1 rounded text-destructive">
                                拥堵
                              </span>
                            </div>
                          );
                        } else if (totalVehicles > threshold.c1) {
                          return (
                            <div className="flex items-center justify-between">
                              <span className="text-xs text-muted-foreground">
                                密度：
                              </span>
                              <span className="font-medium text-xs sm:text-sm bg-amber-500/10 px-2 py-1 rounded text-amber-700 dark:text-amber-400">
                                较拥挤
                              </span>
                            </div>
                          );
                        }
                        return (
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-muted-foreground">
                              密度：
                            </span>
                            <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                              畅通
                            </span>
                          </div>
                        );
                      })()}
                    </div>

                    {/* Đánh giá về tốc độ */}
                    <div className="p-2 bg-muted/50 rounded">
                      {(() => {
                        const speedFromBackend = trafficData.speed_status;
                        if (speedFromBackend) {
                          if (speedFromBackend === "Nhanh chóng") {
                            return (
                              <div className="flex items-center justify-between">
                                <span className="text-xs text-muted-foreground">
                                  速度：
                                </span>
                                <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                                  较快
                                </span>
                              </div>
                            );
                          }
                          return (
                            <div className="flex items-center justify-between">
                              <span className="text-xs text-muted-foreground">
                                速度：
                              </span>
                              <span className="font-medium text-xs sm:text-sm bg-amber-500/10 px-2 py-1 rounded text-amber-700 dark:text-amber-400">
                                较慢
                              </span>
                            </div>
                          );
                        }

                        // Fallback
                        const threshold = getThresholdForRoad(roadName);
                        const avgSpeed =
                          ((trafficData?.speed_car || 0) +
                            (trafficData?.speed_motor || 0)) /
                          2;
                        if (avgSpeed >= threshold.v) {
                          return (
                            <div className="flex items-center justify-between">
                              <span className="text-xs text-muted-foreground">
                                速度：
                              </span>
                              <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                                较快
                              </span>
                            </div>
                          );
                        }
                        return (
                          <div className="flex items-center justify-between">
                            <span className="text-xs text-muted-foreground">
                              速度：
                            </span>
                            <span className="font-medium text-xs sm:text-sm bg-amber-500/10 px-2 py-1 rounded text-amber-700 dark:text-amber-400">
                              较慢
                            </span>
                          </div>
                        );
                      })()}
                    </div>
                  </div>
                </div>

                {/* Car Section */}
                <div className="mb-2 bg-card p-2 sm:p-3 rounded-lg shadow-sm">
                  <h4 className="text-xs sm:text-sm font-medium mb-2 text-foreground flex items-center gap-2">
                    <Car className="h-3 w-3 sm:h-4 sm:w-4 text-primary" />
                    汽车信息
                  </h4>
                  <div className="space-y-1.5 sm:space-y-2">
                    <div className="flex justify-between items-center p-1.5 sm:p-2 bg-muted/50 rounded">
                      <span className="text-xs text-muted-foreground">
                        数量：
                      </span>
                      <span className="font-medium text-xs sm:text-sm bg-primary/10 px-2 py-1 rounded text-primary">
                        {trafficData?.count_car || 0}
                      </span>
                    </div>
                    <div className="flex justify-between items-center p-1.5 sm:p-2 bg-muted/50 rounded">
                      <span className="text-xs text-muted-foreground">
                        速度：
                      </span>
                      <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                        {trafficData?.speed_car || 0} km/h
                      </span>
                    </div>
                  </div>
                </div>

                {/* Motorcycle Section */}
                <div className="mb-2 bg-card p-2 sm:p-3 rounded-lg shadow-sm">
                  <h4 className="text-xs sm:text-sm font-medium mb-2 text-foreground flex items-center gap-2">
                    <Bike className="h-3 w-3 sm:h-4 sm:w-4 text-emerald-600 dark:text-emerald-400" />
                    摩托车信息
                  </h4>
                  <div className="space-y-1.5 sm:space-y-2">
                    <div className="flex justify-between items-center p-1.5 sm:p-2 bg-muted/50 rounded">
                      <span className="text-xs text-muted-foreground">
                        数量：
                      </span>
                      <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                        {trafficData?.count_motor || 0}
                      </span>
                    </div>
                    <div className="flex justify-between items-center p-1.5 sm:p-2 bg-muted/50 rounded">
                      <span className="text-xs text-muted-foreground">
                        速度：
                      </span>
                      <span className="font-medium text-xs sm:text-sm bg-emerald-500/10 px-2 py-1 rounded text-emerald-700 dark:text-emerald-400">
                        {trafficData?.speed_motor || 0} km/h
                      </span>
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
