# ---- Stage 1: Builder ----
FROM python:3.11-slim AS builder

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
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# System dependencies (single layer, cache cleaned):
#   - libglib2.0-0: OpenCV runtime
#   - ffmpeg: video stream encoding/decoding
#   - libgomp1: OpenVINO threading (OpenMP)
# Note: libgl1 is NOT needed because we use opencv-python-headless.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       libglib2.0-0 \
       ffmpeg \
       libgomp1 \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

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
