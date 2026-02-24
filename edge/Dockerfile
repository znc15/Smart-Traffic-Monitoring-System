FROM python:3.11-slim

WORKDIR /app

# 安装 OpenCV 系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

ENV MODE=sim
ENV ROAD_NAME=示例路段-Edge01
ENV MODEL=yolov8n.pt

CMD ["python", "main.py"]
