FROM python:3.11-slim

WORKDIR /app

# System dependencies:
#   - libgl1, libglib2.0-0: OpenCV runtime
#   - ffmpeg: video stream encoding/decoding
#   - libgomp1: OpenVINO threading (OpenMP)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    ffmpeg \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies (cached layer unless requirements.txt changes)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

EXPOSE 8000

# Defaults — override via docker-compose environment or `docker run -e`
ENV MODE=sim \
    ROAD_NAME=示例路段-Edge01 \
    MODEL=yolov8n.pt \
    NO_BROWSER=true

CMD ["python", "main.py"]
