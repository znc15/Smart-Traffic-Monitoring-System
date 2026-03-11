import {
  $,
  apiFetch,
  appState,
  appendEdgeAccessParams,
  formatUptime,
  setOnlineStatus,
  updateBar,
} from "./core.js";

export const VideoRenderer = {
  video: null,
  fallbackImg: null,
  running: false,
  failCount: 0,
  maxFails: 3,
  useMJPEG: false,

  init() {
    this.video = $("videoStream");
    this.fallbackImg = $("videoStreamFallback");
    if (!this.video || !this.fallbackImg) return;
    this.start();
  },

  start() {
    if (this.running || !this.video || !this.fallbackImg) return;
    this.running = true;
    this.failCount = 0;
    this._connect();
  },

  stop() {
    this.running = false;
    if (this.video) {
      this.video.pause();
      this.video.removeAttribute("src");
      this.video.load();
    }
    if (this.fallbackImg) {
      this.fallbackImg.removeAttribute("src");
    }
  },

  reconnect() {
    this.stop();
    this.useMJPEG = false;
    setTimeout(() => this.start(), 500);
  },

  _connect() {
    if (!this.running || !this.video || !this.fallbackImg) return;

    const placeholder = $("streamPlaceholder");
    if (this.useMJPEG) {
      this.video.style.display = "none";
      this.fallbackImg.style.display = "block";
      this.fallbackImg.src = appendEdgeAccessParams("/api/stream?t=" + Date.now());
      if (placeholder) placeholder.style.display = "none";

      this.fallbackImg.onload = () => {
        this.failCount = 0;
      };
      this.fallbackImg.onerror = () => this._onFallbackError();
      return;
    }

    this.fallbackImg.style.display = "none";
    this.video.style.display = "block";
    if (placeholder) placeholder.style.display = "none";

    this.video.src = appendEdgeAccessParams("/api/video?t=" + Date.now());
    this.video.onloadeddata = () => {
      this.failCount = 0;
      this.video.play().catch(() => {});
    };
    this.video.onerror = () => this._onVideoError();
  },

  _onVideoError() {
    this.failCount += 1;
    if (this.failCount >= 2) {
      console.warn("[VideoRenderer] WebM 视频流不可用，回退到 MJPEG");
      this.useMJPEG = true;
      this.failCount = 0;
      this._connect();
      return;
    }
    if (this.running) {
      setTimeout(() => this._connect(), 1000);
    }
  },

  _onFallbackError() {
    this.failCount += 1;
    if (this.failCount >= this.maxFails) {
      this._showPlaceholder();
    } else if (this.running) {
      setTimeout(() => this._connect(), 1000);
    }
  },

  _showPlaceholder() {
    this.running = false;
    if (this.video) this.video.style.display = "none";
    if (this.fallbackImg) this.fallbackImg.style.display = "none";

    const placeholder = $("streamPlaceholder");
    if (placeholder) placeholder.style.display = "flex";
  },
};

export function reconnectStream() {
  VideoRenderer.reconnect();
}

function initChart() {
  const canvas = $("trendChart");
  const ChartLib = window.Chart;
  if (!canvas || !ChartLib) return;

  const ctx = canvas.getContext("2d");
  appState.trendChart = new ChartLib(ctx, {
    type: "line",
    data: {
      labels: appState.chartLabels,
      datasets: [
        {
          label: "汽车",
          data: appState.chartCarData,
          borderColor: "#3b82f6",
          backgroundColor: "rgba(59,130,246,0.1)",
          borderWidth: 2,
          pointRadius: 0,
          tension: 0.3,
          fill: true,
        },
        {
          label: "摩托/非机动车",
          data: appState.chartMotorData,
          borderColor: "#10b981",
          backgroundColor: "rgba(16,185,129,0.1)",
          borderWidth: 2,
          pointRadius: 0,
          tension: 0.3,
          fill: true,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 400 },
      interaction: { intersect: false, mode: "index" },
      plugins: {
        legend: {
          labels: { color: "#64748b", boxWidth: 12, padding: 16 },
        },
      },
      scales: {
        x: {
          ticks: { color: "#94a3b8", maxTicksLimit: 10, maxRotation: 0 },
          grid: { color: "rgba(226,232,240,0.8)" },
        },
        y: {
          beginAtZero: true,
          ticks: { color: "#94a3b8", stepSize: 1 },
          grid: { color: "rgba(226,232,240,0.8)" },
        },
      },
    },
  });
}

function appendChartData(carCount, motorCount) {
  const now = new Date();
  const label = now.getHours().toString().padStart(2, "0") + ":"
    + now.getMinutes().toString().padStart(2, "0") + ":"
    + now.getSeconds().toString().padStart(2, "0");

  appState.chartLabels.push(label);
  appState.chartCarData.push(carCount);
  appState.chartMotorData.push(motorCount);

  if (appState.chartLabels.length > appState.MAX_POINTS) {
    appState.chartLabels.shift();
    appState.chartCarData.shift();
    appState.chartMotorData.shift();
  }

  if (appState.trendChart) {
    appState.trendChart.update();
  }
}

export async function fetchTrafficData() {
  try {
    const resp = await apiFetch("/api/traffic");
    if (!resp.ok) throw new Error("HTTP " + resp.status);

    const data = await resp.json();
    const m = data.edge_metrics || {};

    setOnlineStatus(true);

    const countCar = $("countCar");
    const countMotor = $("countMotor");
    const speedCar = $("speedCar");
    const speedMotor = $("speedMotor");
    if (countCar) countCar.textContent = data.count_car || 0;
    if (countMotor) countMotor.textContent = data.count_motor || 0;
    if (speedCar) speedCar.textContent = (data.speed_car || 0).toFixed(1);
    if (speedMotor) speedMotor.textContent = (data.speed_motor || 0).toFixed(1);

    updateBar("cpuBar", "cpuValue", m.cpu_percent);
    updateBar("memBar", "memValue", m.memory_percent);
    updateBar("gpuBar", "gpuValue", m.gpu_percent);

    const fps = m.fps != null ? m.fps.toFixed(1) : "--";
    const inf = m.inference_ms != null ? m.inference_ms.toFixed(1) : "--";

    const fpsValue = $("fpsValue");
    const inferenceValue = $("inferenceValue");
    const streamFps = $("streamFps");
    const streamInference = $("streamInference");
    const modelName = $("modelName");
    const uptime = $("uptime");

    if (fpsValue) fpsValue.textContent = fps;
    if (inferenceValue) inferenceValue.textContent = inf;
    if (streamFps) streamFps.textContent = fps;
    if (streamInference) streamInference.textContent = inf;
    if (modelName) modelName.textContent = m.model || "--";
    if (uptime) uptime.textContent = m.uptime_s != null ? formatUptime(m.uptime_s) : "--:--:--";

    appendChartData(data.count_car || 0, data.count_motor || 0);
  } catch (err) {
    console.warn("数据获取失败:", err.message);
    setOnlineStatus(false);
  }
}

export function initMonitorModule() {
  initChart();
  fetchTrafficData();

  if (appState.trafficPollTimer) {
    clearInterval(appState.trafficPollTimer);
  }
  appState.trafficPollTimer = window.setInterval(fetchTrafficData, appState.POLL_INTERVAL);

  VideoRenderer.init();
}
