const pick = <T>(obj: Record<string, unknown>, snake: string, camel: string, fallback: T): T => {
  const snakeVal = obj[snake]
  if (snakeVal !== undefined && snakeVal !== null) return snakeVal as T
  const camelVal = obj[camel]
  if (camelVal !== undefined && camelVal !== null) return camelVal as T
  return fallback
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
