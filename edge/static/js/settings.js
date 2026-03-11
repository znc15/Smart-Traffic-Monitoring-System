import {
  $,
  apiFetch,
  appState,
  debounce,
  escapeHtml,
  registerSettingsLoader,
  saveEdgeAccessKey,
  showToast,
  updateEdgeAccessHint,
} from "./core.js";
import { reconnectStream } from "./monitor.js";

async function loadSettingsData(options = {}) {
  const opts = options || {};
  if (appState.settingsLoaded && !opts.force) {
    return;
  }

  try {
    const [configResp, modelsResp, resResp] = await Promise.all([
      apiFetch("/api/config"),
      apiFetch("/api/models"),
      apiFetch("/api/resource-mode"),
    ]);

    if (!configResp.ok) throw new Error("获取配置失败");
    if (!modelsResp.ok) throw new Error("获取模型列表失败");

    const config = await configResp.json();
    const modelsData = await modelsResp.json();

    appState.currentConfig = config;
    appState.settingsLoaded = true;
    updateEdgeAccessHint(!!config.edge_api_key_configured);

    populateModels(modelsData.models || [], modelsData.current || config.model);
    fillSettingsForm(config);

    if (resResp.ok) {
      const resData = await resResp.json();
      appState.resourceParams = resData;
      appState.currentResourceLevel = resData.level || "medium";
      updateResourceUI(resData);
      fillPerfTuningForm({ ...resData, ...config });
    } else {
      fillPerfTuningForm(config);
    }
  } catch (err) {
    console.error("加载设置失败:", err);
    showToast("加载设置失败: " + err.message, "error");
  }
}

function populateModels(models, currentModel) {
  const select = $("settingModel");
  if (!select) return;

  select.innerHTML = "";
  if (models.length === 0) {
    select.innerHTML = "<option value=\"\">无可用模型</option>";
    return;
  }

  models.forEach((model) => {
    const opt = document.createElement("option");
    opt.value = model.name;
    const sizeMB = model.size_mb ? ` (${model.size_mb.toFixed(1)} MB)` : "";
    const ov = model.has_openvino ? " [OV缓存]" : "";
    opt.textContent = model.name + sizeMB + ov;
    if (model.name === currentModel) opt.selected = true;
    select.appendChild(opt);
  });
}

function fillSettingsForm(config) {
  setMode(config.mode || "sim");

  const sourceInput = $("settingCameraSource");
  const openVino = $("settingOpenVINO");
  const confidence = $("settingConfidence");
  const confidenceDisplay = $("confidenceDisplay");
  const roadName = $("settingRoadName");

  if (sourceInput) sourceInput.value = config.camera_source || "";
  if (openVino) openVino.checked = !!config.use_openvino;

  const conf = config.confidence || 0.5;
  if (confidence) confidence.value = conf;
  if (confidenceDisplay) confidenceDisplay.textContent = conf.toFixed(2);
  if (roadName) roadName.value = config.road_name || "";
}

export function setMode(mode) {
  const simBtn = $("modeSimBtn");
  const camBtn = $("modeCameraBtn");
  const sourceInput = $("settingCameraSource");
  const hint = $("cameraSourceHint");

  if (mode === "sim") {
    if (simBtn) simBtn.classList.add("active");
    if (camBtn) camBtn.classList.remove("active");
    if (sourceInput) sourceInput.disabled = true;
    if (hint) hint.textContent = "模拟模式下无需配置摄像头源";
  } else {
    if (simBtn) simBtn.classList.remove("active");
    if (camBtn) camBtn.classList.add("active");
    if (sourceInput) sourceInput.disabled = false;
    if (hint) hint.textContent = "支持 RTSP 地址、设备编号（如 0）或本地视频文件路径";
  }

  settings_updateTestBtn();
  updateModeUI(mode);
}

export async function toggleMode() {
  const newMode = appState.currentMode === "sim" ? "camera" : "sim";
  if (newMode === "camera") {
    const source = $("settingCameraSource") ? $("settingCameraSource").value.trim() : "";
    if (!source) {
      const url = window.prompt("请输入摄像头源（设备索引如 0，或 RTSP URL）：");
      if (!url || !url.trim()) return;
      if ($("settingCameraSource")) $("settingCameraSource").value = url.trim();
    }
  }
  await applyModeSwitch(newMode);
}

export function switchToSim() {
  if (appState.currentMode === "sim") return;
  applyModeSwitch("sim");
}

export function switchToCamera() {
  if (appState.currentMode === "camera") return;

  const source = $("settingCameraSource") ? $("settingCameraSource").value.trim() : "";
  if (!source) {
    const url = window.prompt("请输入摄像头源（设备索引如 0，或 RTSP URL）：");
    if (!url || !url.trim()) return;
    if ($("settingCameraSource")) $("settingCameraSource").value = url.trim();
  }

  applyModeSwitch("camera");
}

async function applyModeSwitch(mode) {
  const payload = { mode };
  if (mode === "camera") {
    const src = $("settingCameraSource") ? $("settingCameraSource").value.trim() : "";
    if (src) payload.camera_source = src;
  }

  try {
    const resp = await apiFetch("/api/config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) {
      const errData = await resp.json().catch(() => ({}));
      throw new Error(errData.detail || errData.error || "HTTP " + resp.status);
    }

    const result = await resp.json();
    appState.currentConfig = result;
    appState.settingsLoaded = true;
    updateEdgeAccessHint(!!result.edge_api_key_configured);

    updateModeUI(mode);

    const simBtn = $("modeSimBtn");
    const camBtn = $("modeCameraBtn");
    if (simBtn && camBtn) {
      simBtn.classList.toggle("active", mode === "sim");
      camBtn.classList.toggle("active", mode === "camera");
    }

    const sourceInput = $("settingCameraSource");
    if (sourceInput) sourceInput.disabled = mode === "sim";

    showToast(mode === "sim" ? "已切换到模拟模式" : "已切换到摄像头模式", "success");
    setTimeout(() => reconnectStream(), 2000);
  } catch (err) {
    console.error("模式切换失败:", err);
    showToast("模式切换失败: " + err.message, "error");
  }
}

function updateModeUI(mode) {
  appState.currentMode = mode;

  const badge = $("navModeBadge");
  if (badge) {
    badge.className = "nav-mode-badge " + mode;
    badge.textContent = mode === "sim" ? "模拟模式" : "生产模式";
  }

  const statusText = $("modeStatusText");
  const statusDesc = $("modeStatusDesc");
  if (statusText) {
    statusText.textContent = mode === "sim" ? "当前模式：模拟模式" : "当前模式：生产模式";
  }
  if (statusDesc) {
    if (mode === "sim") {
      statusDesc.textContent = "使用示例素材进行推理演示";
    } else {
      const src = $("settingCameraSource") ? $("settingCameraSource").value.trim() : "";
      statusDesc.textContent = "摄像头实时检测" + (src ? " - 源: " + src : "");
    }
  }

  const switchSim = $("switchSimBtn");
  const switchCam = $("switchCameraBtn");
  if (switchSim) switchSim.classList.toggle("active", mode === "sim");
  if (switchCam) switchCam.classList.toggle("active", mode === "camera");
}

function getSelectedMode() {
  const simBtn = $("modeSimBtn");
  return simBtn && simBtn.classList.contains("active") ? "sim" : "camera";
}

export async function applySettings() {
  const btn = $("applyBtn");
  const payload = {
    mode: getSelectedMode(),
    camera_source: $("settingCameraSource") ? $("settingCameraSource").value.trim() : "",
    model: $("settingModel") ? $("settingModel").value : "",
    confidence: $("settingConfidence") ? parseFloat($("settingConfidence").value) : 0.5,
    road_name: $("settingRoadName") ? $("settingRoadName").value.trim() : "",
    use_openvino: $("settingOpenVINO") ? $("settingOpenVINO").checked : false,
  };

  if (btn) {
    btn.disabled = true;
    btn.innerHTML = "<span class=\"spinner\"></span> 切换中...";
  }

  try {
    const resp = await apiFetch("/api/config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) {
      const errData = await resp.json().catch(() => ({}));
      throw new Error(errData.detail || errData.error || "HTTP " + resp.status);
    }

    const result = await resp.json();
    appState.currentConfig = result;
    appState.settingsLoaded = true;
    updateEdgeAccessHint(!!result.edge_api_key_configured);
    showToast("设置已应用，系统重启中...", "success");

    setTimeout(() => {
      window.switchTab("monitor");
      reconnectStream();
    }, 3000);
  } catch (err) {
    console.error("应用设置失败:", err);
    showToast("应用设置失败: " + err.message, "error");
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = "应用设置";
    }
  }
}

export async function resetSettings() {
    appState.settingsLoaded = false;
    await loadSettingsData({ force: true });

  const listContainer = $("cameraListContainer");
  if (listContainer) {
    listContainer.style.display = "none";
    listContainer.innerHTML = "";
  }

  const probeResult = $("probeResult");
  if (probeResult) probeResult.style.display = "none";

  showToast("已重置为当前运行配置", "success");
}

export async function settings_scanCameras() {
  const btn = $("scanCamerasBtn");
  const container = $("cameraListContainer");
  const camIcon = "<svg width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z\"/><circle cx=\"12\" cy=\"13\" r=\"4\"/></svg>";
  if (!btn || !container) return;

  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    container.innerHTML = "<p style=\"font-size:0.8rem;color:#dc2626;padding:6px 0;\">当前浏览器不支持摄像头访问。请使用 HTTPS 或 localhost 访问，或换用现代浏览器。</p>";
    container.style.display = "flex";
    return;
  }

  btn.disabled = true;
  btn.innerHTML = "<span class=\"spinner\" style=\"width:12px;height:12px;border-width:1.5px;\"></span> 请求权限...";

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ video: true });
    stream.getTracks().forEach((track) => track.stop());

    btn.innerHTML = "<span class=\"spinner\" style=\"width:12px;height:12px;border-width:1.5px;\"></span> 枚举设备...";

    const devices = await navigator.mediaDevices.enumerateDevices();
    const videoDevices = devices.filter((device) => device.kind === "videoinput");

    container.innerHTML = "";
    if (videoDevices.length === 0) {
      container.innerHTML = "<p style=\"font-size:0.8rem;color:#64748b;padding:6px 0;\">未发现本地摄像头</p>";
    } else {
      videoDevices.forEach((device, idx) => {
        const item = document.createElement("div");
        item.className = "camera-item";
        item.dataset.index = idx;
        item.setAttribute("tabindex", "0");
        item.setAttribute("role", "button");

        const label = device.label || ("摄像头 #" + idx);
        item.setAttribute("aria-label", "选择 " + label);
        item.innerHTML = "<div class=\"cam-label\">" + escapeHtml(label) + "</div>"
          + "<div class=\"cam-info\">设备索引: " + idx + "</div>";
        item.onclick = () => settings_selectCamera(idx, item);
        item.onkeydown = (event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            settings_selectCamera(idx, item);
          }
        };
        container.appendChild(item);
      });
    }

    container.style.display = "flex";
  } catch (err) {
    console.error("扫描摄像头失败:", err);
    container.innerHTML = "";

    let msg = "扫描失败: " + err.message;
    if (err.name === "NotAllowedError") {
      msg = "摄像头权限被拒绝，请在浏览器设置中允许摄像头访问后重试。";
    } else if (err.name === "NotFoundError") {
      msg = "未检测到摄像头设备。";
    } else if (err.name === "NotReadableError") {
      msg = "摄像头被其他程序占用，请关闭后重试。";
    }

    const errP = document.createElement("p");
    errP.style.cssText = "font-size:0.8rem;color:#dc2626;padding:6px 0;";
    errP.textContent = msg;
    container.appendChild(errP);
    container.style.display = "flex";
  } finally {
    btn.disabled = false;
    btn.innerHTML = camIcon + " 扫描本地摄像头";
  }
}

export async function settings_scanCamerasServer() {
  const btn = $("scanCamerasServerBtn");
  const container = $("cameraListContainer");
  const serverIcon = "<svg width=\"12\" height=\"12\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"2\" y=\"2\" width=\"20\" height=\"8\" rx=\"2\" ry=\"2\"/><rect x=\"2\" y=\"14\" width=\"20\" height=\"8\" rx=\"2\" ry=\"2\"/><line x1=\"6\" y1=\"6\" x2=\"6.01\" y2=\"6\"/><line x1=\"6\" y1=\"18\" x2=\"6.01\" y2=\"18\"/></svg>";
  if (!btn || !container) return;

  btn.disabled = true;
  btn.innerHTML = "<span class=\"spinner\" style=\"width:12px;height:12px;border-width:1.5px;\"></span> 扫描中...";

  try {
    const resp = await apiFetch("/api/cameras");
    if (!resp.ok) throw new Error("HTTP " + resp.status);

    const data = await resp.json();
    const cameras = data.cameras || [];

    container.innerHTML = "";
    if (cameras.length === 0) {
      container.innerHTML = "<p style=\"font-size:0.8rem;color:#64748b;padding:6px 0;\">服务端未发现摄像头设备</p>";
    } else {
      cameras.forEach((cam) => {
        const item = document.createElement("div");
        item.className = "camera-item";
        item.dataset.index = cam.index;
        item.setAttribute("tabindex", "0");
        item.setAttribute("role", "button");
        item.setAttribute("aria-label", "选择摄像头 #" + cam.index);

        const res = (cam.width && cam.height)
          ? escapeHtml(String(cam.width)) + "x" + escapeHtml(String(cam.height))
          : "未知分辨率";
        const fps = cam.fps ? escapeHtml(String(cam.fps.toFixed(0))) + "fps" : "";
        item.innerHTML = "<div class=\"cam-label\">摄像头 #" + escapeHtml(String(cam.index)) + " (服务端)</div>"
          + "<div class=\"cam-info\">" + res + (fps ? " @ " + fps : "") + "</div>";
        item.onclick = () => settings_selectCamera(cam.index, item);
        item.onkeydown = (event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            settings_selectCamera(cam.index, item);
          }
        };
        container.appendChild(item);
      });
    }

    container.style.display = "flex";
  } catch (err) {
    console.error("服务端扫描摄像头失败:", err);
    container.innerHTML = "";
    const errP = document.createElement("p");
    errP.style.cssText = "font-size:0.8rem;color:#dc2626;padding:6px 0;";
    errP.textContent = "服务端扫描失败: " + err.message;
    container.appendChild(errP);
    container.style.display = "flex";
  } finally {
    btn.disabled = false;
    btn.innerHTML = serverIcon + " 服务端扫描（RTSP/网络摄像头）";
  }
}

function settings_selectCamera(index, el) {
  const listContainer = $("cameraListContainer");
  const sourceInput = $("settingCameraSource");
  if (!listContainer || !sourceInput) return;

  listContainer.querySelectorAll(".camera-item").forEach((item) => item.classList.remove("selected"));
  el.classList.add("selected");
  sourceInput.value = String(index);
  setMode("camera");
  settings_updateTestBtn();

  const labelEl = el.querySelector(".cam-label");
  const label = labelEl ? labelEl.textContent : ("摄像头 #" + index);
  showToast("已选择: " + label, "success");
}

export function settings_updateTestBtn() {
  const input = $("settingCameraSource");
  const btn = $("probeBtn");
  if (!input || !btn) return;

  const val = input.value.trim();
  const isUrl = val.includes("://") || val.startsWith("http");
  btn.disabled = !isUrl || !val;
}

export async function settings_probeConnection() {
  const btn = $("probeBtn");
  const resultEl = $("probeResult");
  const sourceInput = $("settingCameraSource");
  if (!btn || !resultEl || !sourceInput) return;

  const url = sourceInput.value.trim();
  if (!url) return;

  btn.disabled = true;
  btn.textContent = "测试中...";
  resultEl.style.display = "none";

  try {
    const resp = await apiFetch("/api/cameras/probe", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ url }),
    });
    if (!resp.ok) throw new Error("HTTP " + resp.status);

    const data = await resp.json();
    if (data.reachable) {
      const res = (data.width && data.height) ? " (" + data.width + "x" + data.height + ")" : "";
      resultEl.className = "probe-result success";
      resultEl.textContent = "连接成功" + res;
    } else {
      resultEl.className = "probe-result fail";
      resultEl.textContent = "无法连接" + (data.message ? ": " + data.message : "");
    }
  } catch (err) {
    resultEl.className = "probe-result fail";
    resultEl.textContent = "测试失败: " + err.message;
  }

  resultEl.style.display = "block";
  btn.disabled = false;
  btn.textContent = "测试";
  settings_updateTestBtn();
  setTimeout(() => {
    resultEl.style.display = "none";
  }, 3000);
}

export async function settings_previewCamera() {
  const sourceInput = $("settingCameraSource");
  const btn = $("previewBtn");
  const resultEl = $("previewResult");
  const previewImage = $("previewImage");
  const previewStats = $("previewStats");
  if (!sourceInput || !btn || !resultEl || !previewImage || !previewStats) return;

  const source = sourceInput.value.trim();
  if (!source) {
    showToast("请先输入摄像头源", "error");
    return;
  }

  btn.disabled = true;
  btn.textContent = "抓帧中...";
  resultEl.style.display = "none";

  try {
    const resp = await apiFetch("/api/cameras/preview?source=" + encodeURIComponent(source));
    if (!resp.ok) {
      const err = await resp.json();
      throw new Error(err.detail || "HTTP " + resp.status);
    }

    const blob = await resp.blob();
    const countCar = resp.headers.get("X-Count-Car") || "0";
    const countMotor = resp.headers.get("X-Count-Motor") || "0";
    const inferenceMs = resp.headers.get("X-Inference-Ms") || "0";

    previewImage.src = URL.createObjectURL(blob);
    previewStats.textContent = "检测结果: 汽车 " + countCar + " 辆, 摩托/自行车 " + countMotor + " 辆, 推理耗时 " + inferenceMs + "ms";
    resultEl.style.display = "block";
    showToast("抓帧测试成功", "success");
  } catch (err) {
    showToast("抓帧测试失败: " + err.message, "error");
  } finally {
    btn.disabled = false;
    btn.textContent = "抓帧测试";
  }
}

async function fetchResourceMode() {
  try {
    const resp = await apiFetch("/api/resource-mode");
    if (!resp.ok) throw new Error("HTTP " + resp.status);

    const data = await resp.json();
    appState.resourceParams = data;
    appState.currentResourceLevel = data.level || "medium";
    updateResourceUI(data);
  } catch (err) {
    console.warn("Failed to fetch resource mode:", err.message);
  }
}

function updateResourceUI(data) {
  const level = data.level || "medium";
  const cores = data.cpu_cores || "--";
  const memGb = data.memory_total_gb || "--";

  const monBadge = $("monitorResourceBadge");
  const monLabel = $("monitorResourceLabel");
  if (monBadge && monLabel) {
    monBadge.className = "resource-badge " + level;
    monLabel.textContent = level;
  }

  const perfBadge = $("perfResourceBadge");
  const perfLabel = $("perfResourceLabel");
  if (perfBadge && perfLabel) {
    perfBadge.className = "resource-badge " + level;
    perfLabel.textContent = level;
  }

  const hwInfo = $("perfHardwareInfo");
  if (hwInfo) hwInfo.textContent = cores + " cores / " + memGb + " GB RAM";

  const chips = $("perfResourceChips");
  if (chips) {
    chips.textContent = "";
    const chipData = [
      ["Frame Skip", data.FRAME_SKIP || 1],
      ["IMGSZ", data.IMGSZ || 640],
      ["JPEG", data.JPEG_QUALITY || 80],
      ["Clients", data.MAX_MJPEG_CLIENTS || 4],
    ];
    chipData.forEach((pair) => {
      const span = document.createElement("span");
      span.className = "resource-chip";
      span.textContent = pair[0] + ": " + pair[1];
      chips.appendChild(span);
    });
  }
}

function fillPerfTuningForm(cfg) {
  const fs = cfg.frame_skip || cfg.FRAME_SKIP || 1;
  const perfFrameSkip = $("perfFrameSkip");
  const perfFrameSkipDisplay = $("perfFrameSkipDisplay");
  const perfFrameSkipValue = $("perfFrameSkipValue");
  if (perfFrameSkip) perfFrameSkip.value = fs;
  if (perfFrameSkipDisplay) perfFrameSkipDisplay.textContent = fs;
  if (perfFrameSkipValue) perfFrameSkipValue.textContent = fs;

  const imgsz = cfg.imgsz || cfg.IMGSZ || 640;
  const perfImgsz = $("perfImgsz");
  if (perfImgsz) perfImgsz.value = String(imgsz);

  const jpegQ = cfg.JPEG_QUALITY || cfg.jpeg_quality || 80;
  const perfJpegQuality = $("perfJpegQuality");
  const perfJpegQualityDisplay = $("perfJpegQualityDisplay");
  const perfJpegQualityValue = $("perfJpegQualityValue");
  if (perfJpegQuality) perfJpegQuality.value = jpegQ;
  if (perfJpegQualityDisplay) perfJpegQualityDisplay.textContent = jpegQ;
  if (perfJpegQualityValue) perfJpegQualityValue.textContent = jpegQ;

  const q = cfg.quantize || cfg.QUANTIZE || "none";
  const perfQuantize = $("perfQuantize");
  if (perfQuantize) perfQuantize.value = q;
}

async function updatePerfParam(key, value) {
  const payload = {};
  payload[key] = value;

  try {
    const resp = await apiFetch("/api/config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) {
      const errData = await resp.json().catch(() => ({}));
      throw new Error(errData.detail || "HTTP " + resp.status);
    }

    const result = await resp.json();
    appState.currentConfig = result;
    showToast("参数已更新: " + key + " = " + value, "success");
    fetchResourceMode();
  } catch (err) {
    console.error("Failed to update " + key + ":", err);
    showToast("更新失败: " + err.message, "error");
  }
}

const debouncedUpdatePerf = debounce(updatePerfParam, 500);

function initPerfTuningListeners() {
  const fsSlider = $("perfFrameSkip");
  if (fsSlider) {
    fsSlider.addEventListener("input", function onInput() {
      const display = $("perfFrameSkipDisplay");
      const value = $("perfFrameSkipValue");
      if (display) display.textContent = this.value;
      if (value) value.textContent = this.value;
    });
    fsSlider.addEventListener("change", function onChange() {
      debouncedUpdatePerf("frame_skip", parseInt(this.value, 10));
    });
  }

  const imgszSelect = $("perfImgsz");
  if (imgszSelect) {
    imgszSelect.addEventListener("change", function onChange() {
      updatePerfParam("imgsz", parseInt(this.value, 10));
    });
  }

  const jpegSlider = $("perfJpegQuality");
  if (jpegSlider) {
    jpegSlider.addEventListener("input", function onInput() {
      const display = $("perfJpegQualityDisplay");
      const value = $("perfJpegQualityValue");
      if (display) display.textContent = this.value;
      if (value) value.textContent = this.value;
    });
    jpegSlider.addEventListener("change", function onChange() {
      debouncedUpdatePerf("jpeg_quality", parseInt(this.value, 10));
    });
  }

  const quantizeSelect = $("perfQuantize");
  if (quantizeSelect) {
    quantizeSelect.addEventListener("change", function onChange() {
      updatePerfParam("quantize", this.value);
    });
  }
}

export function initSettingsModule() {
  registerSettingsLoader(loadSettingsData);

  const slider = $("settingConfidence");
  if (slider) {
    slider.addEventListener("input", function onInput() {
      const confidenceDisplay = $("confidenceDisplay");
      if (confidenceDisplay) {
        confidenceDisplay.textContent = parseFloat(this.value).toFixed(2);
      }
    });
  }

  fetchResourceMode().then(() => {
    if (appState.resourceParams && Object.keys(appState.resourceParams).length > 0) {
      fillPerfTuningForm(appState.resourceParams);
    }
  });

  initPerfTuningListeners();
  settings_updateTestBtn();

  const keyInput = $("edgeAccessKey");
  if (keyInput) {
    keyInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") {
        event.preventDefault();
        saveEdgeAccessKey();
      }
    });
  }
}
