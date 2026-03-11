export const TOKEN_KEY = 'access_token'

type EnvLike = Record<string, string | undefined>

function readEnv(): EnvLike {
  return (((import.meta as ImportMeta & { env?: EnvLike }).env) || {}) as EnvLike
}

const env = readEnv()

export const AMAP_KEY = String(env.VITE_AMAP_KEY || '')

function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, '')
}

function resolveHttpBaseRoot(): string {
  const configured = trimTrailingSlash(String(env.VITE_API_HTTP_BASE || ''))
  if (configured) return configured
  if (typeof window !== 'undefined') {
    return trimTrailingSlash(window.location.origin)
  }
  return 'http://127.0.0.1:8000'
}

function resolveWsBaseRoot(): string {
  const configured = trimTrailingSlash(String(env.VITE_API_WS_BASE || ''))
  if (configured) return configured
  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}`
  }
  return 'ws://127.0.0.1:8000'
}

const HTTP_BASE_ROOT = resolveHttpBaseRoot()
const WS_BASE_ROOT = resolveWsBaseRoot()

export const API_HTTP_BASE = `${HTTP_BASE_ROOT}/api/v1`
export const API_WS_BASE = `${WS_BASE_ROOT}/api/v1`

export const endpoints = {
  login: `${API_HTTP_BASE}/auth/login`,
  register: `${API_HTTP_BASE}/auth/register`,
  me: `${API_HTTP_BASE}/auth/me`,
  siteSettings: `${API_HTTP_BASE}/site-settings`,
  roads: `${API_HTTP_BASE}/roads_name`,
  mapOverview: `${API_HTTP_BASE}/map/overview`,
  infoWs: (roadName: string) => `${API_WS_BASE}/ws/info/${encodeURIComponent(roadName)}`,
  frameNoAuth: (roadName: string) => `${API_HTTP_BASE}/frames_no_auth/${encodeURIComponent(roadName)}`,
  reportExport: `${API_HTTP_BASE}/reports/traffic/export`,
  adminUsers: `${API_HTTP_BASE}/admin/users`,
  adminCameras: `${API_HTTP_BASE}/admin/cameras`,
  adminSiteSettings: `${API_HTTP_BASE}/admin/site-settings`,
  adminResources: `${API_HTTP_BASE}/admin/resources`,
  adminNodes: `${API_HTTP_BASE}/admin/nodes`,
  adminEvents: `${API_HTTP_BASE}/admin/events`,
  adminNodeConfig: (cameraId: number) => `${API_HTTP_BASE}/admin/nodes/${cameraId}/config`,
  adminApiClients: `${API_HTTP_BASE}/admin/api-clients`,
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export async function authFetch(
  url: string,
  init: RequestInit = {},
  timeoutMs = 30_000,
) {
  const token = getToken()
  const headers = new Headers(init.headers || {})
  if (!headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
  if (init.signal) {
    init.signal.addEventListener('abort', () => controller.abort())
  }

  try {
    const res = await fetch(url, {
      ...init,
      headers,
      credentials: 'include',
      signal: controller.signal,
    })

    if (res.status === 401) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
      throw new Error('Unauthorized')
    }

    return res
  } finally {
    clearTimeout(timeoutId)
  }
}

export function getWsUrl(pathOrUrl: string): string {
  const token = getToken()
  if (!token) return pathOrUrl
  const separator = pathOrUrl.includes('?') ? '&' : '?'
  return `${pathOrUrl}${separator}token=${encodeURIComponent(token)}`
}
