"""
轻量级跟踪器（ByteTrack 不可用时的简化回退实现）
按 IoU 为检测结果分配稳定 track_id，满足跨帧去重统计需求。
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, List, Tuple


BBox = Tuple[int, int, int, int]


@dataclass
class TrackState:
    bbox: BBox
    stale_frames: int = 0


class SimpleTracker:
    def __init__(self, iou_threshold: float = 0.3, max_stale_frames: int = 15):
        self.iou_threshold = iou_threshold
        self.max_stale_frames = max_stale_frames
        self._next_id = 1
        self._tracks: Dict[int, TrackState] = {}

    def update(self, objects: List[dict]) -> List[dict]:
        assigned: Dict[int, int] = {}  # obj_idx -> track_id
        used_tracks = set()

        # 逐目标找最大 IoU 的历史轨迹
        for obj_idx, obj in enumerate(objects):
            bbox = tuple(obj.get("bbox", [0, 0, 0, 0]))  # type: ignore[assignment]
            best_track_id = None
            best_iou = 0.0
            for track_id, state in self._tracks.items():
                if track_id in used_tracks:
                    continue
                score = _iou(bbox, state.bbox)
                if score > best_iou:
                    best_iou = score
                    best_track_id = track_id

            if best_track_id is not None and best_iou >= self.iou_threshold:
                assigned[obj_idx] = best_track_id
                used_tracks.add(best_track_id)
                self._tracks[best_track_id].bbox = bbox
                self._tracks[best_track_id].stale_frames = 0
            else:
                track_id = self._next_id
                self._next_id += 1
                assigned[obj_idx] = track_id
                used_tracks.add(track_id)
                self._tracks[track_id] = TrackState(bbox=bbox, stale_frames=0)

        # 未命中的历史轨迹衰减
        for track_id, state in list(self._tracks.items()):
            if track_id not in used_tracks:
                state.stale_frames += 1
                if state.stale_frames > self.max_stale_frames:
                    del self._tracks[track_id]

        enriched = []
        for idx, obj in enumerate(objects):
            enriched_obj = dict(obj)
            enriched_obj["track_id"] = assigned[idx]
            enriched.append(enriched_obj)
        return enriched


def _iou(a: BBox, b: BBox) -> float:
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b

    inter_x1 = max(ax1, bx1)
    inter_y1 = max(ay1, by1)
    inter_x2 = min(ax2, bx2)
    inter_y2 = min(ay2, by2)

    inter_w = max(0, inter_x2 - inter_x1)
    inter_h = max(0, inter_y2 - inter_y1)
    inter_area = inter_w * inter_h

    area_a = max(0, ax2 - ax1) * max(0, ay2 - ay1)
    area_b = max(0, bx2 - bx1) * max(0, by2 - by1)
    union_area = area_a + area_b - inter_area

    if union_area <= 0:
        return 0.0
    return inter_area / union_area
