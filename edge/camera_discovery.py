"""
摄像头自动发现与交互选择
扫描本地设备和 RTSP 源，让用户选择要使用的摄像头
"""

import cv2


# ---------------------------------------------------------------------------
# 扫描本地摄像头设备
# ---------------------------------------------------------------------------
def scan_local_cameras(max_index: int = 10) -> list[dict]:
    """
    探测本地摄像头设备（索引 0 ~ max_index-1）
    返回可用设备列表，每项包含 index, width, height, fps
    """
    devices = []
    for i in range(max_index):
        cap = cv2.VideoCapture(i)
        try:
            if cap.isOpened():
                w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                fps = cap.get(cv2.CAP_PROP_FPS)
                devices.append({
                    "index": i,
                    "width": w,
                    "height": h,
                    "fps": round(fps, 1) if fps > 0 else 0,
                })
        finally:
            cap.release()
    return devices


# ---------------------------------------------------------------------------
# 检测 RTSP 源是否可用
# ---------------------------------------------------------------------------
def probe_rtsp(url: str, timeout_ms: int = 5000) -> dict | None:
    """
    尝试连接 RTSP 地址，返回分辨率信息；失败返回 None
    """
    cap = cv2.VideoCapture(url)
    try:
        # 设置超时（部分后端支持）
        cap.set(cv2.CAP_PROP_OPEN_TIMEOUT_MSEC, timeout_ms)
        if not cap.isOpened():
            return None
        w = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        h = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        return {"url": url, "width": w, "height": h}
    finally:
        cap.release()


# ---------------------------------------------------------------------------
# 交互式选择
# ---------------------------------------------------------------------------
def interactive_select() -> str | int:
    """
    扫描可用摄像头并让用户交互选择
    返回: 设备索引(int) 或 RTSP 地址(str)
    """
    print("\n" + "=" * 50)
    print("  摄像头自动发现")
    print("=" * 50)
    print("\n正在扫描本地摄像头...")

    devices = scan_local_cameras()

    if devices:
        print(f"\n发现 {len(devices)} 个本地摄像头:\n")
        for i, dev in enumerate(devices):
            fps_str = f"{dev['fps']}fps" if dev['fps'] > 0 else "未知fps"
            print(f"  [{i + 1}] 设备 {dev['index']}  "
                  f"{dev['width']}x{dev['height']}  {fps_str}")
    else:
        print("\n未发现本地摄像头")

    # 额外选项
    offset = len(devices)
    print(f"\n  [{offset + 1}] 手动输入 RTSP/视频地址")
    print(f"  [{offset + 2}] 使用模拟模式")
    print()

    while True:
        try:
            choice = input("请选择 [编号]: ").strip()
            if not choice:
                continue
            num = int(choice)
        except (ValueError, EOFError):
            print("请输入有效编号")
            continue

        # 选择了本地摄像头
        if 1 <= num <= len(devices):
            selected = devices[num - 1]
            print(f"\n已选择: 设备 {selected['index']} "
                  f"({selected['width']}x{selected['height']})")
            return selected["index"]

        # 手动输入地址
        if num == offset + 1:
            url = input("请输入视频地址 (RTSP/HTTP/文件路径): ").strip()
            if not url:
                print("地址不能为空")
                continue
            print(f"正在检测: {url} ...")
            info = probe_rtsp(url)
            if info:
                print(f"连接成功: {info['width']}x{info['height']}")
            else:
                print("无法连接，但仍可尝试使用")
            return url

        # 模拟模式
        if num == offset + 2:
            return "sim"

        print("编号无效，请重新选择")
