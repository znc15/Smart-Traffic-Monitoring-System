import {
  $,
  apiFetch,
  appState,
  appendEdgeAccessParams,
  escapeHtml,
  showToast,
} from "./core.js";

const TEST_IMAGE_EXTS = ["jpg", "jpeg", "png", "bmp", "webp"];
const TEST_VIDEO_EXTS = ["mp4", "avi", "mov"];
const TEST_IMAGE_MAX_SIZE = 10 * 1024 * 1024;
const TEST_VIDEO_MAX_SIZE = 100 * 1024 * 1024;

function test_getFileType(file) {
  const ext = file.name.split(".").pop().toLowerCase();
  if (TEST_IMAGE_EXTS.includes(ext)) return "image";
  if (TEST_VIDEO_EXTS.includes(ext)) return "video";
  if (file.type.startsWith("image/")) return "image";
  if (file.type.startsWith("video/")) return "video";
  return null;
}

function test_validateFile(file) {
  const type = test_getFileType(file);
  if (!type) {
    showToast("不支持的文件格式，请上传图片或视频文件", "error");
    return null;
  }

  const maxSize = type === "image" ? TEST_IMAGE_MAX_SIZE : TEST_VIDEO_MAX_SIZE;
  if (file.size > maxSize) {
    const limitMB = maxSize / (1024 * 1024);
    showToast(`文件过大，${type === "image" ? "图片" : "视频"}最大 ${limitMB}MB`, "error");
    return null;
  }

  return type;
}

function test_formatSize(bytes) {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

async function test_detectImage(file) {
  const originalDataUrl = await new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = (event) => resolve(event.target.result);
    reader.readAsDataURL(file);
  });

  try {
    const formData = new FormData();
    formData.append("file", file);

    const resp = await apiFetch("/api/detect/image", { method: "POST", body: formData });
    if (!resp.ok) {
      const errData = await resp.json().catch(() => ({}));
      throw new Error(errData.detail || errData.error || "HTTP " + resp.status);
    }

    const data = await resp.json();
    if (!data.success) throw new Error(data.error || "检测失败");

    $("testLoading").style.display = "none";
    $("testImageResult").style.display = "block";
    $("testOriginalImg").src = originalDataUrl;
    $("testAnnotatedImg").src = data.annotated_image;

    const det = data.detections || {};
    $("testCountCar").textContent = det.count_car || 0;
    $("testCountMotor").textContent = det.count_motor || 0;
    $("testInferenceMs").textContent = (det.inference_ms || 0).toFixed(1);
    test_renderObjectsTable(det.objects || []);

    appState.testCurrentResult = { type: "image", data, originalDataUrl, fileName: file.name };
    test_addHistory({
      type: "image",
      fileName: file.name,
      fileSize: file.size,
      thumbnail: originalDataUrl,
      summary: `汽车 ${det.count_car || 0} / 摩托 ${det.count_motor || 0} / ${(det.inference_ms || 0).toFixed(1)}ms`,
      time: new Date(),
      result: { data, originalDataUrl },
    });

    showToast("图片检测完成", "success");
  } catch (err) {
    console.error("图片检测失败:", err);
    $("testLoading").style.display = "none";
    showToast("图片检测失败: " + err.message, "error");
  }
}

function test_renderObjectsTable(objects) {
  const tbody = $("testObjectsTable");
  const noObj = $("testNoObjects");
  if (!tbody || !noObj) return;

  if (!objects || objects.length === 0) {
    tbody.innerHTML = "";
    noObj.style.display = "block";
    return;
  }

  noObj.style.display = "none";
  tbody.innerHTML = objects.map((obj, i) => {
    const conf = (obj.confidence * 100).toFixed(1);
    const bbox = obj.bbox ? obj.bbox.map((v) => Math.round(v)).join(", ") : "--";
    const bgClass = i % 2 === 0 ? "bg-white" : "bg-slate-50";
    const classMap = { car: "汽车", motor: "摩托/非机动车", truck: "卡车", bus: "公交车" };
    const className = classMap[obj.class] || obj.class;

    return `<tr class="${bgClass}">
      <td class="px-4 py-2">${escapeHtml(className)}</td>
      <td class="px-4 py-2"><span class="text-blue-500">${conf}%</span></td>
      <td class="px-4 py-2 font-mono text-xs text-slate-500">${escapeHtml(bbox)}</td>
    </tr>`;
  }).join("");
}

async function test_detectVideo(file) {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const resp = await apiFetch("/api/detect/video", { method: "POST", body: formData });
    if (!resp.ok) {
      const errData = await resp.json().catch(() => ({}));
      throw new Error(errData.detail || errData.error || "HTTP " + resp.status);
    }

    const data = await resp.json();
    if (!data.success) throw new Error(data.error || "视频处理失败");

    $("testLoading").style.display = "none";
    $("testVideoResult").style.display = "block";

    const s = data.summary || {};
    $("testTotalFrames").textContent = s.total_frames || 0;
    $("testProcessedFrames").textContent = s.processed_frames || 0;
    $("testAvgCar").textContent = (s.avg_car_count || 0).toFixed(1);
    $("testAvgMotor").textContent = (s.avg_motor_count || 0).toFixed(1);
    $("testAvgInference").textContent = (s.avg_inference_ms || 0).toFixed(1);
    $("testDuration").textContent = (s.duration_s || 0).toFixed(1);

    const videoUrl = appendEdgeAccessParams(data.video_url || "");
    $("testVideoPlayer").src = videoUrl;
    $("testVideoDownload").href = videoUrl;

    appState.testCurrentResult = { type: "video", data, fileName: file.name };
    test_addHistory({
      type: "video",
      fileName: file.name,
      fileSize: file.size,
      thumbnail: null,
      summary: `帧 ${s.processed_frames || 0}/${s.total_frames || 0} / 车均 ${(s.avg_car_count || 0).toFixed(1)} / ${(s.duration_s || 0).toFixed(1)}s`,
      time: new Date(),
      result: { data },
    });

    showToast("视频处理完成", "success");
  } catch (err) {
    console.error("视频检测失败:", err);
    $("testLoading").style.display = "none";
    showToast("视频检测失败: " + err.message, "error");
  }
}

function test_handleFile(file) {
  const type = test_validateFile(file);
  if (!type) return;

  $("testResultArea").style.display = "block";
  $("testLoading").style.display = "block";
  $("testImageResult").style.display = "none";
  $("testVideoResult").style.display = "none";

  if (type === "image") {
    $("testLoadingText").textContent = "图片检测中...";
    test_detectImage(file);
  } else {
    $("testLoadingText").textContent = "视频处理中，请稍候...";
    test_detectVideo(file);
  }
}

function test_addHistory(entry) {
  appState.testHistory.unshift(entry);
  if (appState.testHistory.length > 10) appState.testHistory.pop();
  test_renderHistory();
}

function test_renderHistory() {
  const list = $("testHistoryList");
  const empty = $("testHistoryEmpty");
  const count = $("testHistoryCount");
  if (!list || !empty || !count) return;

  count.textContent = appState.testHistory.length + " / 10";
  if (appState.testHistory.length === 0) {
    empty.style.display = "block";
    Array.from(list.children).forEach((child) => {
      if (child.id !== "testHistoryEmpty") child.remove();
    });
    return;
  }

  empty.style.display = "none";
  const fragment = document.createDocumentFragment();

  appState.testHistory.forEach((entry, index) => {
    const item = document.createElement("div");
    item.className = "flex items-center gap-3 bg-white border border-slate-200 rounded-lg p-3 cursor-pointer hover:border-blue-300 transition-colors";
    item.onclick = () => test_viewHistory(index);

    let thumbHtml;
    if (entry.thumbnail) {
      thumbHtml = `<img src="${entry.thumbnail}" class="w-12 h-12 rounded object-cover flex-shrink-0" alt="缩略图">`;
    } else {
      thumbHtml = `<div class="w-12 h-12 rounded bg-slate-100 flex items-center justify-center flex-shrink-0">
        <svg class="w-6 h-6 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
        </svg>
      </div>`;
    }

    const typeLabel = entry.type === "image" ? "图片" : "视频";
    const typeColor = entry.type === "image" ? "text-blue-600 bg-blue-50" : "text-emerald-600 bg-emerald-50";
    const t = entry.time;
    const timeStr = t.getHours().toString().padStart(2, "0") + ":"
      + t.getMinutes().toString().padStart(2, "0") + ":"
      + t.getSeconds().toString().padStart(2, "0");

    item.innerHTML = `
      ${thumbHtml}
      <div class="flex-1 min-w-0">
        <div class="flex items-center gap-2 mb-0.5">
          <span class="text-sm text-slate-700 truncate">${escapeHtml(entry.fileName)}</span>
          <span class="text-xs px-1.5 py-0.5 rounded ${typeColor} flex-shrink-0">${typeLabel}</span>
        </div>
        <p class="text-xs text-slate-500 truncate">${escapeHtml(entry.summary)}</p>
      </div>
      <div class="text-xs text-slate-400 flex-shrink-0">${timeStr}</div>
    `;

    fragment.appendChild(item);
  });

  Array.from(list.children).forEach((child) => {
    if (child.id !== "testHistoryEmpty") child.remove();
  });
  list.appendChild(fragment);
}

function test_viewHistory(index) {
  const entry = appState.testHistory[index];
  if (!entry) return;

  $("testResultArea").style.display = "block";
  $("testLoading").style.display = "none";

  if (entry.type === "image") {
    $("testImageResult").style.display = "block";
    $("testVideoResult").style.display = "none";

    const r = entry.result;
    $("testOriginalImg").src = r.originalDataUrl;
    $("testAnnotatedImg").src = r.data.annotated_image;

    const det = r.data.detections || {};
    $("testCountCar").textContent = det.count_car || 0;
    $("testCountMotor").textContent = det.count_motor || 0;
    $("testInferenceMs").textContent = (det.inference_ms || 0).toFixed(1);
    test_renderObjectsTable(det.objects || []);
  } else {
    $("testImageResult").style.display = "none";
    $("testVideoResult").style.display = "block";

    const r = entry.result;
    const s = r.data.summary || {};
    $("testTotalFrames").textContent = s.total_frames || 0;
    $("testProcessedFrames").textContent = s.processed_frames || 0;
    $("testAvgCar").textContent = (s.avg_car_count || 0).toFixed(1);
    $("testAvgMotor").textContent = (s.avg_motor_count || 0).toFixed(1);
    $("testAvgInference").textContent = (s.avg_inference_ms || 0).toFixed(1);
    $("testDuration").textContent = (s.duration_s || 0).toFixed(1);

    const videoUrl = appendEdgeAccessParams(r.data.video_url || "");
    $("testVideoPlayer").src = videoUrl;
    $("testVideoDownload").href = videoUrl;
  }

  $("testResultArea").scrollIntoView({ behavior: "smooth", block: "start" });
}

function test_initDropZone() {
  const dropZone = $("testDropZone");
  const fileInput = $("testFileInput");
  const overlay = $("testDropOverlay");
  if (!dropZone || !fileInput || !overlay) return;

  fileInput.addEventListener("change", function onChange(event) {
    const file = event.target.files[0];
    if (file) test_handleFile(file);
    this.value = "";
  });

  dropZone.addEventListener("dragenter", (event) => {
    event.preventDefault();
    event.stopPropagation();
    overlay.style.display = "flex";
    dropZone.classList.add("border-blue-500");
  });

  dropZone.addEventListener("dragover", (event) => {
    event.preventDefault();
    event.stopPropagation();
    overlay.style.display = "flex";
    dropZone.classList.add("border-blue-500");
  });

  dropZone.addEventListener("dragleave", (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (!dropZone.contains(event.relatedTarget)) {
      overlay.style.display = "none";
      dropZone.classList.remove("border-blue-500");
    }
  });

  dropZone.addEventListener("drop", (event) => {
    event.preventDefault();
    event.stopPropagation();
    overlay.style.display = "none";
    dropZone.classList.remove("border-blue-500");

    const files = event.dataTransfer.files;
    if (files.length > 0) {
      test_handleFile(files[0]);
    }
  });
}

export function initTestPanel() {
  test_initDropZone();
  void test_formatSize;
}
