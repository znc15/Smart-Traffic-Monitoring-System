/**
 * 各路段交通阈值配置
 *
 * - v: 平均速度阈值 (km/h) - 大于等于 v 为畅通，否则为缓慢
 * - c1: 车辆数量低阈值 - 大于 c1 为拥挤
 * - c2: 车辆数量高阈值 - 大于 c2 为拥堵
 *
 * 分类规则：
 * - 拥堵：车辆总数 > c2
 * - 拥挤：车辆总数 > c1 且 <= c2
 * - 畅通：车辆总数 <= c1
 */

export interface RoadThreshold {
  v: number; // 平均速度阈值
  c1: number; // 拥挤阈值
  c2: number; // 拥堵阈值
}

export const TRAFFIC_THRESHOLDS: Record<string, RoadThreshold> = {
  "郎路": {
    v: 6,
    c1: 17,
    c2: 26,
  },
  "四所交叉口": {
    v: 17,
    c1: 45,
    c2: 57,
  },
  "阮廌路": {
    v: 30,
    c1: 25,
    c2: 35,
  },
  "文馆": {
    v: 10,
    c1: 10,
    c2: 17,
  },
  "文富": {
    v: 13,
    c1: 18,
    c2: 26,
  },
};

/**
 * 未配置路段的默认阈值
 */
export const DEFAULT_THRESHOLD: RoadThreshold = {
  v: 15,
  c1: 15,
  c2: 25,
};

/**
 * 获取指定路段的阈值
 * @param roadName - 路段名称
 * @returns 该路段的阈值或默认阈值
 */
export const getThresholdForRoad = (roadName: string): RoadThreshold => {
  return TRAFFIC_THRESHOLDS[roadName] || DEFAULT_THRESHOLD;
};
