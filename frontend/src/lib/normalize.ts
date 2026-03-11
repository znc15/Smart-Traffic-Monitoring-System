const pick = <T>(obj: Record<string, unknown>, snake: string, camel: string, fallback: T): T => {
  const snakeVal = obj[snake]
  if (snakeVal !== undefined && snakeVal !== null) return snakeVal as T
  const camelVal = obj[camel]
  if (camelVal !== undefined && camelVal !== null) return camelVal as T
  return fallback
}

function parseRecordPayload(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object') {
    return value as Record<string, unknown>
  }
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      return parsed && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {}
    } catch {
      return {}
    }
  }
  return {}
}

export type TrafficInfo = {
  count_car: number
  count_motor: number
  count_person: number
  speed_car: number
  speed_motor: number
  density_status: string
  speed_status: string
  online: boolean
}

export function normalizeTrafficInfo(raw: unknown): TrafficInfo {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    count_car: Number(pick(obj, 'count_car', 'countCar', 0)),
    count_motor: Number(pick(obj, 'count_motor', 'countMotor', 0)),
    count_person: Number(pick(obj, 'count_person', 'countPerson', 0)),
    speed_car: Number(pick(obj, 'speed_car', 'speedCar', 0)),
    speed_motor: Number(pick(obj, 'speed_motor', 'speedMotor', 0)),
    density_status: String(pick(obj, 'density_status', 'densityStatus', 'unknown')),
    speed_status: String(pick(obj, 'speed_status', 'speedStatus', 'unknown')),
    online: Boolean(pick(obj, 'online', 'online', true))
  }
}

export type AdminUser = {
  id: number
  username: string
  email: string
  phone_number: string
  role_id: number
  enabled: boolean
}

export function normalizeAdminUser(raw: unknown): AdminUser {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    id: Number(obj.id || 0),
    username: String(obj.username || ''),
    email: String(obj.email || ''),
    phone_number: String(pick(obj, 'phone_number', 'phoneNumber', '')),
    role_id: Number(pick(obj, 'role_id', 'roleId', 1)),
    enabled: Boolean(obj.enabled)
  }
}

export type CameraItem = {
  id: number
  name: string
  location: string
  stream_url: string
  road_name: string
  node_url: string
  edge_node_id: string
  node_api_key: string
  enabled: boolean
  latitude: number | null
  longitude: number | null
}

export function normalizeCamera(raw: unknown): CameraItem {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    id: Number(obj.id || 0),
    name: String(obj.name || ''),
    location: String(obj.location || ''),
    stream_url: String(pick(obj, 'stream_url', 'streamUrl', '')),
    road_name: String(pick(obj, 'road_name', 'roadName', '')),
    node_url: String(pick(obj, 'node_url', 'nodeUrl', '')),
    edge_node_id: String(pick(obj, 'edge_node_id', 'edgeNodeId', '')),
    node_api_key: String(pick(obj, 'node_api_key', 'nodeApiKey', '')),
    enabled: Boolean(obj.enabled),
    latitude: obj.latitude === null || obj.latitude === undefined ? null : Number(obj.latitude),
    longitude: obj.longitude === null || obj.longitude === undefined ? null : Number(obj.longitude)
  }
}

export type SiteSettings = {
  site_name: string
  announcement: string
  logo_url: string
  footer_text: string
}

export function normalizeSiteSettings(raw: unknown): SiteSettings {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    site_name: String(pick(obj, 'site_name', 'siteName', '智能交通监控系统')),
    announcement: String(obj.announcement || ''),
    logo_url: String(pick(obj, 'logo_url', 'logoUrl', '')),
    footer_text: String(pick(obj, 'footer_text', 'footerText', ''))
  }
}

export type ApiClient = {
  id: number
  name: string
  api_key: string
  description: string
  allowed_endpoints: string[]
  rate_limit: number
  enabled: boolean
  last_used_at: string | null
  created_at: string
}

export function normalizeApiClient(raw: unknown): ApiClient {
  const obj = (raw || {}) as Record<string, unknown>
  const allowedRaw = pick(obj, 'allowed_endpoints', 'allowedEndpoints', [] as unknown)
  return {
    id: Number(obj.id || 0),
    name: String(obj.name || ''),
    api_key: String(pick(obj, 'api_key', 'apiKey', '')),
    description: String(obj.description || ''),
    allowed_endpoints: Array.isArray(allowedRaw) ? (allowedRaw as string[]) : [],
    rate_limit: Number(pick(obj, 'rate_limit', 'rateLimit', 1000)),
    enabled: Boolean(obj.enabled !== false),
    last_used_at: pick(obj, 'last_used_at', 'lastUsedAt', null) as string | null,
    created_at: String(pick(obj, 'created_at', 'createdAt', '')),
  }
}

export type ApiClientUsage = {
  total_calls: number
  daily_stats: { date: string; count: number }[]
  endpoint_stats: { endpoint: string; count: number }[]
}

export function normalizeApiClientUsage(raw: unknown): ApiClientUsage {
  const obj = (raw || {}) as Record<string, unknown>
  const dailyRaw = pick(obj, 'daily_stats', 'dailyStats', [] as unknown)
  const endpointRaw = pick(obj, 'endpoint_stats', 'endpointStats', [] as unknown)
  return {
    total_calls: Number(pick(obj, 'total_calls', 'totalCalls', 0)),
    daily_stats: Array.isArray(dailyRaw)
      ? (dailyRaw as Record<string, unknown>[]).map((d) => ({
          date: String(d.date || ''),
          count: Number(d.count || 0),
        }))
      : [],
    endpoint_stats: Array.isArray(endpointRaw)
      ? (endpointRaw as Record<string, unknown>[]).map((e) => ({
          endpoint: String(e.endpoint || ''),
          count: Number(e.count || 0),
        }))
      : [],
  }
}

export type MapOverviewPoint = {
  camera_id: number
  name: string
  road_name: string
  edge_node_id: string
  latitude: number
  longitude: number
  online: boolean
  density_status: string
  congestion_index: number
  count_car: number
  count_motor: number
  count_person: number
  speed_car: number
  speed_motor: number
  snapshot_url: string
  updated_at: string | null
}

export function normalizeMapOverviewPoint(raw: unknown): MapOverviewPoint {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    camera_id: Number(pick(obj, 'camera_id', 'cameraId', 0)),
    name: String(obj.name || ''),
    road_name: String(pick(obj, 'road_name', 'roadName', '')),
    edge_node_id: String(pick(obj, 'edge_node_id', 'edgeNodeId', '')),
    latitude: Number(obj.latitude || 0),
    longitude: Number(obj.longitude || 0),
    online: Boolean(obj.online),
    density_status: String(pick(obj, 'density_status', 'densityStatus', 'unknown')),
    congestion_index: Number(pick(obj, 'congestion_index', 'congestionIndex', 0)),
    count_car: Number(pick(obj, 'count_car', 'countCar', 0)),
    count_motor: Number(pick(obj, 'count_motor', 'countMotor', 0)),
    count_person: Number(pick(obj, 'count_person', 'countPerson', 0)),
    speed_car: Number(pick(obj, 'speed_car', 'speedCar', 0)),
    speed_motor: Number(pick(obj, 'speed_motor', 'speedMotor', 0)),
    snapshot_url: String(
      pick(obj, 'snapshot_url', 'snapshotUrl', String(pick(obj, 'frame_url', 'frameUrl', ''))),
    ),
    updated_at: pick(obj, 'updated_at', 'updatedAt', null) as string | null,
  }
}

export type NodeRuntimeConfig = {
  road_name: string
  mode: string
  camera_source: string
  analysis_roi: unknown
  lane_split_ratios: number[]
  speed_meters_per_pixel: number
  parking_stationary_seconds: number
  wrong_way_min_track_points: number
  telemetry_interval_sec: number
}

export function normalizeNodeRuntimeConfig(raw: unknown): NodeRuntimeConfig {
  const obj = (raw || {}) as Record<string, unknown>
  const ratios = pick(obj, 'lane_split_ratios', 'laneSplitRatios', [] as unknown)
  return {
    road_name: String(pick(obj, 'road_name', 'roadName', '')),
    mode: String(obj.mode || ''),
    camera_source: String(pick(obj, 'camera_source', 'cameraSource', '')),
    analysis_roi: pick(obj, 'analysis_roi', 'analysisRoi', null),
    lane_split_ratios: Array.isArray(ratios) ? ratios.map((value) => Number(value || 0)) : [],
    speed_meters_per_pixel: Number(
      pick(obj, 'speed_meters_per_pixel', 'speedMetersPerPixel', 0),
    ),
    parking_stationary_seconds: Number(
      pick(obj, 'parking_stationary_seconds', 'parkingStationarySeconds', 0),
    ),
    wrong_way_min_track_points: Number(
      pick(obj, 'wrong_way_min_track_points', 'wrongWayMinTrackPoints', 0),
    ),
    telemetry_interval_sec: Number(
      pick(obj, 'telemetry_interval_sec', 'telemetryIntervalSec', 0),
    ),
  }
}

export type NodeHealthStatus = 'online' | 'degraded' | 'offline'
export type NodeStatusReasonCode =
  | 'auth_failed'
  | 'timeout'
  | 'traffic_fetch_failed'
  | 'frame_fetch_failed'
  | null
export type NodeErrorStage = 'traffic' | 'frame' | null

export type AdminNodeHealth = {
  camera_id: number
  name: string
  road_name: string
  edge_node_id: string
  node_url: string
  online: boolean
  health_status: NodeHealthStatus
  status_reason_code: NodeStatusReasonCode
  status_reason_message: string | null
  last_error_stage: NodeErrorStage
  last_success_time: string | null
  last_poll_time: string | null
  latency_ms: number | null
  error_count: number
  consecutive_failures: number
  last_error: string | null
  edge_metrics: Record<string, unknown> | null
}

export function normalizeAdminNodeHealth(raw: unknown): AdminNodeHealth {
  const obj = (raw || {}) as Record<string, unknown>
  const healthStatusRaw = String(
    pick(obj, 'health_status', 'healthStatus', String(obj.online ? 'online' : 'offline')),
  )
  const normalizedHealthStatus: NodeHealthStatus =
    healthStatusRaw === 'degraded' || healthStatusRaw === 'offline' ? healthStatusRaw : 'online'
  const reasonCodeRaw = pick(
    obj,
    'status_reason_code',
    'statusReasonCode',
    null as unknown,
  )
  const normalizedReasonCode: NodeStatusReasonCode =
    reasonCodeRaw === 'auth_failed' ||
    reasonCodeRaw === 'timeout' ||
    reasonCodeRaw === 'traffic_fetch_failed' ||
    reasonCodeRaw === 'frame_fetch_failed'
      ? reasonCodeRaw
      : null
  const errorStageRaw = pick(obj, 'last_error_stage', 'lastErrorStage', null as unknown)
  const normalizedErrorStage: NodeErrorStage =
    errorStageRaw === 'traffic' || errorStageRaw === 'frame' ? errorStageRaw : null

  return {
    camera_id: Number(pick(obj, 'camera_id', 'cameraId', 0)),
    name: String(obj.name || ''),
    road_name: String(pick(obj, 'road_name', 'roadName', '')),
    edge_node_id: String(pick(obj, 'edge_node_id', 'edgeNodeId', '')),
    node_url: String(pick(obj, 'node_url', 'nodeUrl', '')),
    online: Boolean(obj.online),
    health_status: normalizedHealthStatus,
    status_reason_code: normalizedReasonCode,
    status_reason_message:
      pick(obj, 'status_reason_message', 'statusReasonMessage', null) == null
        ? null
        : String(pick(obj, 'status_reason_message', 'statusReasonMessage', '')),
    last_error_stage: normalizedErrorStage,
    last_success_time: pick(obj, 'last_success_time', 'lastSuccessTime', null) as string | null,
    last_poll_time: pick(obj, 'last_poll_time', 'lastPollTime', null) as string | null,
    latency_ms:
      pick(obj, 'latency_ms', 'latencyMs', null) == null
        ? null
        : Number(pick(obj, 'latency_ms', 'latencyMs', 0)),
    error_count: Number(pick(obj, 'error_count', 'errorCount', 0)),
    consecutive_failures: Number(
      pick(obj, 'consecutive_failures', 'consecutiveFailures', 0),
    ),
    last_error:
      pick(obj, 'last_error', 'lastError', null) == null
        ? null
        : String(pick(obj, 'last_error', 'lastError', '')),
    edge_metrics:
      obj.edge_metrics && typeof obj.edge_metrics === 'object'
        ? (obj.edge_metrics as Record<string, unknown>)
        : obj.edgeMetrics && typeof obj.edgeMetrics === 'object'
          ? (obj.edgeMetrics as Record<string, unknown>)
          : null,
  }
}

export type TrafficEventItem = {
  id: number
  road_name: string
  edge_node_id: string
  event_type: string
  level: string
  start_at: string | null
  end_at: string | null
  created_at: string | null
  payload: Record<string, unknown>
}

export function normalizeTrafficEvent(raw: unknown): TrafficEventItem {
  const obj = (raw || {}) as Record<string, unknown>
  return {
    id: Number(obj.id || 0),
    road_name: String(pick(obj, 'road_name', 'roadName', '')),
    edge_node_id: String(pick(obj, 'edge_node_id', 'edgeNodeId', '')),
    event_type: String(pick(obj, 'event_type', 'eventType', '')),
    level: String(obj.level || ''),
    start_at: pick(obj, 'start_at', 'startAt', null) as string | null,
    end_at: pick(obj, 'end_at', 'endAt', null) as string | null,
    created_at: pick(obj, 'created_at', 'createdAt', null) as string | null,
    payload: parseRecordPayload(
      pick(
        obj,
        'payload_json',
        'payloadJson',
        obj.payload,
      ),
    ),
  }
}
