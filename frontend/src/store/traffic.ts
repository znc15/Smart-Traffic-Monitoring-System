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
  state.initialized = true
}

export async function refreshRoads(): Promise<string[]> {
  const res = await fetch(endpoints.roads, { credentials: 'include' })
  if (!res.ok) {
    return state.roads
  }

  const body = await res.json()
  const freshRoads: string[] = Array.isArray(body?.road_names) ? body.road_names : []

  const currentRoads = new Set(state.roads)
  const newRoads = freshRoads.filter((road) => !currentRoads.has(road))

  state.roads = freshRoads
  newRoads.forEach((road) => connectRoad(road))

  return freshRoads
}

export function closeTrafficStore() {
  state.initialized = false

  for (const timer of reconnectTimers.values()) clearTimeout(timer)
  reconnectTimers.clear()
  reconnectCounts.clear()

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
