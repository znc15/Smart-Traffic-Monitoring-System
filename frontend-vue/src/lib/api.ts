export const TOKEN_KEY = 'access_token'

const HTTP_BASE_ROOT = import.meta.env.VITE_API_HTTP_BASE || 'http://localhost:8000'
const WS_BASE_ROOT = import.meta.env.VITE_API_WS_BASE || 'ws://localhost:8000'

export const API_HTTP_BASE = `${HTTP_BASE_ROOT}/api/v1`
export const API_WS_BASE = `${WS_BASE_ROOT}/api/v1`

export const endpoints = {
  login: `${API_HTTP_BASE}/auth/login`,
  me: `${API_HTTP_BASE}/auth/me`,
  siteSettings: `${API_HTTP_BASE}/site-settings`,
  roads: `${API_HTTP_BASE}/roads_name`,
  infoWs: (roadName: string) => `${API_WS_BASE}/ws/info/${encodeURIComponent(roadName)}`,
  frameNoAuth: (roadName: string) => `${API_HTTP_BASE}/frames_no_auth/${encodeURIComponent(roadName)}`,
  reportExport: `${API_HTTP_BASE}/reports/traffic/export`,
  adminUsers: `${API_HTTP_BASE}/admin/users`,
  adminCameras: `${API_HTTP_BASE}/admin/cameras`,
  adminSiteSettings: `${API_HTTP_BASE}/admin/site-settings`,
  adminResources: `${API_HTTP_BASE}/admin/resources`,
  adminNodes: `${API_HTTP_BASE}/admin/nodes`
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

export async function authFetch(url: string, init: RequestInit = {}) {
  const token = getToken()
  const headers = new Headers(init.headers || {})
  if (!headers.has('Content-Type') && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  return fetch(url, {
    ...init,
    headers,
    credentials: 'include'
  })
}

export function getWsUrl(pathOrUrl: string): string {
  const token = getToken()
  if (!token) return pathOrUrl
  const separator = pathOrUrl.includes('?') ? '&' : '?'
  return `${pathOrUrl}${separator}token=${encodeURIComponent(token)}`
}
