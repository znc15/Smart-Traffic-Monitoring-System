ARG PYTHON_BASE_IMAGE=python:3.11-slim

# ---- Stage 1: Builder ----
FROM ${PYTHON_BASE_IMAGE} AS builder

WORKDIR /build

COPY requirements.txt .

# Install CPU-only torch first via PyTorch index, then the rest.
# --no-cache-dir keeps the layer lean.
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt \
    && find /install -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null; \
    find /install -type d -name "*.dist-info" -exec rm -rf {} + 2>/dev/null; \
    find /install -type d -name "tests" -exec rm -rf {} + 2>/dev/null; \
    # Remove CUDA/cuDNN stubs that torch ships even in CPU builds
    find /install -name "*.so" -path "*/nvidia/*" -delete 2>/dev/null; \
    true

# ---- Stage 2: Runtime ----
FROM ${PYTHON_BASE_IMAGE}

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# System dependencies (single layer, cache cleaned):
#   - libglib2.0-0: OpenCV runtime
#   - ffmpeg: video stream encoding/decoding
#   - libgomp1: OpenVINO threading (OpenMP)
# Note: libgl1 is NOT needed because we use opencv-python-headless.
RUN set -ex; \
        APT_GET_OPTS='-o Acquire::Retries=5 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 -o Acquire::http::Pipeline-Depth=0'; \
        apt-get ${APT_GET_OPTS} update; \
        success=0; \
        for attempt in 1 2 3; do \
            if apt-get ${APT_GET_OPTS} install -y --no-install-recommends --fix-missing \
                     libglib2.0-0 \
                     ffmpeg \
                     libgomp1; then \
                success=1; \
                break; \
            fi; \
            echo "apt-get install 失败，准备重试（第 ${attempt}/3 次）..." >&2; \
            rm -rf /var/lib/apt/lists/*; \
            apt-get ${APT_GET_OPTS} update; \
        done; \
        test "${success}" = "1"; \
        rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Copy pre-built Python packages from builder stage
COPY --from=builder /install /usr/local

# Copy application code
COPY . .

EXPOSE 8000

# Defaults -- override via docker-compose environment or `docker run -e`
ENV MODE=sim \
    ROAD_NAME=demo-road-Edge01 \
    MODEL=yolov8n.pt \
    NO_BROWSER=true

CMD ["python", "main.py"]
