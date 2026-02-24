#!/usr/bin/env python3
"""
YOLOv8 模型下载脚本
将常用模型下载到 edge/models/ 目录，供检测模块使用

用法:
    python download_models.py                    # 下载默认模型 (yolov8n.pt, yolov8s.pt)
    python download_models.py yolov8m.pt         # 下载指定模型
    python download_models.py yolov8n.pt yolov8s.pt yolov8m.pt  # 下载多个模型
"""

import sys

from ultralytics import YOLO

from config import MODELS_DIR

# 默认下载的模型列表
DEFAULT_MODELS = ["yolov8n.pt", "yolov8s.pt"]


def download_model(model_name: str) -> bool:
    """
    下载单个 YOLOv8 模型到 models/ 目录
    返回是否成功
    """
    target_path = MODELS_DIR / model_name

    # 如果模型已存在，跳过下载
    if target_path.exists():
        size_mb = target_path.stat().st_size / (1024 * 1024)
        print(f"[跳过] {model_name} 已存在 ({size_mb:.1f} MB)")
        return True

    print(f"[下载] 正在下载 {model_name} ...")
    try:
        # 直接指定完整路径，ultralytics 会自动下载到该位置
        YOLO(str(target_path))
        size_mb = target_path.stat().st_size / (1024 * 1024)
        print(f"[完成] {model_name} 下载成功 ({size_mb:.1f} MB)")
        return True
    except Exception as e:
        print(f"[失败] {model_name} 下载失败: {e}")
        return False


def main() -> None:
    """主函数：解析命令行参数并下载模型"""
    # 确保 models/ 目录存在
    MODELS_DIR.mkdir(exist_ok=True)

    # 从命令行参数获取模型列表，无参数时使用默认列表
    models = sys.argv[1:] if len(sys.argv) > 1 else DEFAULT_MODELS

    print(f"模型存放目录: {MODELS_DIR.resolve()}")
    print(f"待下载模型: {', '.join(models)}")
    print("-" * 50)

    success_count = 0
    fail_count = 0

    for model_name in models:
        # 基本校验：只允许 .pt 文件名，禁止路径分隔符
        if "/" in model_name or "\\" in model_name:
            print(f"[错误] 无效的模型名称（不允许路径分隔符）: {model_name}")
            fail_count += 1
            continue

        if not model_name.endswith(".pt"):
            print(f"[警告] {model_name} 不是 .pt 文件，跳过")
            fail_count += 1
            continue

        if download_model(model_name):
            success_count += 1
        else:
            fail_count += 1

    print("-" * 50)
    print(f"下载完成: 成功 {success_count}, 失败 {fail_count}")


if __name__ == "__main__":
    main()
