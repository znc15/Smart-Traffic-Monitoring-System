import { reactive } from 'vue'
import { endpoints, getWsUrl } from '../lib/api'
import { normalizeTrafficInfo, type TrafficInfo } from '../lib/normalize'

export type HistoricalPoint = { time: string; [k: string]: string | number }

type State = {
  roads: string[]
  trafficData: Record<string, TrafficInfo>
  connections: Record<string, boolean>
  historicalData: HistoricalPoint[]
  initialized: boolean
}

const MAX_HISTORY = 120
const MAX_RECONNECT = 10
const HTTP_FALLBACK_INTERVAL_MS = 3000

const state = reactive<State>({
  roads: [],
  trafficData: {},
  connections: {},
  historicalData: [],
  initialized: false
})

const sockets: Record<string, WebSocket> = {}
const reconnectTimers = new Map<string, ReturnType<typeof setTimeout>>()
const reconnectCounts = new Map<string, number>()
let httpFallbackTimer: ReturnType<typeof setInterval> | null = null

function pushHistory() {
  const point: HistoricalPoint = {
    time: new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }
  state.roads.forEach((road) => {
    const data = state.trafficData[road]
    point[`${road}_total`] = (data?.count_car || 0) + (data?.count_motor || 0)
  })
  state.historicalData = [...state.historicalData, point].slice(-MAX_HISTORY)
}

async function fetchRoadInfo(road: string): Promise<boolean> {
  const res = await fetch(endpoints.info(road), { credentials: 'include' })
  if (!res.ok) {
    return false
  }

  const payload = await res.json()
  state.trafficData[road] = normalizeTrafficInfo(payload)
  return true
}

async function refreshRoadInfos(roads: string[], recordHistory = true): Promise<void> {
  const results = await Promise.allSettled(roads.map((road) => fetchRoadInfo(road)))
  const updated = results.some((result) => result.status === 'fulfilled' && result.value)
  if (updated && recordHistory) {
    pushHistory()
  }
}

function roadsNeedingHttpFallback(): string[] {
  return state.roads.filter((road) => !state.connections[road] || !state.trafficData[road])
}

function startHttpFallbackPoll() {
  if (httpFallbackTimer !== null) return

  httpFallbackTimer = setInterval(() => {
    const roads = roadsNeedingHttpFallback()
    if (roads.length === 0) return
    refreshRoadInfos(roads).catch(() => {
      // Keep the WebSocket reconnect loop responsible for surfacing connection state.
    })
  }, HTTP_FALLBACK_INTERVAL_MS)
}

function connectRoad(road: string) {
  const url = getWsUrl(endpoints.infoWs(road))
  const ws = new WebSocket(url)
  sockets[road] = ws

  ws.onopen = () => {
    state.connections[road] = true
    reconnectCounts.set(road, 0)
  }

  ws.onclose = () => {
    state.connections[road] = false
    if (!state.initialized) return

    fetchRoadInfo(road).catch(() => {
      // HTTP fallback is best-effort; reconnect continues below.
    })

    const attempts = reconnectCounts.get(road) ?? 0
    if (attempts >= MAX_RECONNECT) {
      delete state.connections[road]
      return
    }
    reconnectCounts.set(road, attempts + 1)

    const timer = setTimeout(() => {
      reconnectTimers.delete(road)
      if (state.initialized && state.roads.includes(road)) connectRoad(road)
    }, 1500)
    reconnectTimers.set(road, timer)
  }

  ws.onerror = () => {
    state.connections[road] = false
    fetchRoadInfo(road).catch(() => {
      // HTTP fallback is best-effort.
    })
  }

  ws.onmessage = (ev) => {
    try {
      const payload = JSON.parse(ev.data)
      state.trafficData[road] = normalizeTrafficInfo(payload)
      pushHistory()
    } catch {
      // ignore parse errors
    }
  }
}

export async function initializeTrafficStore() {
  if (state.initialized) return

  const res = await fetch(endpoints.roads, { credentials: 'include' })
  if (!res.ok) {
    state.roads = []
    state.initialized = true
    return
  }

  const body = await res.json()
  const roads = Array.isArray(body?.road_names) ? body.road_names : []
  state.roads = roads
  roads.forEach((road: string) => connectRoad(road))
  await refreshRoadInfos(roads, false)
  startHttpFallbackPoll()
  state.initialized = true
}

export async function refreshRoads(): Promise<string[]> {
  const res = await fetch(endpoints.roads, { credentials: 'include' })
  if (!res.ok) {
    return state.roads
  }

  const body = await res.json()
  const freshRoads: string[] = Array.isArray(body?.road_names) ? body.road_names : []

  const freshSet = new Set(freshRoads)
  const currentRoads = new Set(state.roads)

  // Close connections for roads that no longer exist
  const removedRoads = state.roads.filter((road) => !freshSet.has(road))
  removedRoads.forEach((road) => {
    const timer = reconnectTimers.get(road)
    if (timer !== undefined) {
      clearTimeout(timer)
      reconnectTimers.delete(road)
    }
    reconnectCounts.delete(road)
    const ws = sockets[road]
    if (ws) {
      try {
        ws.close()
      } catch {
        /* ignore */
      }
      delete sockets[road]
    }
    delete state.connections[road]
    delete state.trafficData[road]
  })

  const newRoads = freshRoads.filter((road) => !currentRoads.has(road))

  state.roads = freshRoads
  newRoads.forEach((road) => connectRoad(road))
  await refreshRoadInfos(freshRoads, false)
  startHttpFallbackPoll()

  return freshRoads
}

export function closeTrafficStore() {
  state.initialized = false

  for (const timer of reconnectTimers.values()) clearTimeout(timer)
  reconnectTimers.clear()
  reconnectCounts.clear()
  if (httpFallbackTimer !== null) {
    clearInterval(httpFallbackTimer)
    httpFallbackTimer = null
  }

  Object.values(sockets).forEach((ws) => {
    try {
      ws.close()
    } catch {
      // ignore
    }
  })
  for (const k of Object.keys(sockets)) delete sockets[k]

  state.roads = []
  state.trafficData = {}
  state.connections = {}
  state.historicalData = []
}

export function useTrafficStoreState() {
  return state
}
