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
const state = reactive<State>({
  roads: [],
  trafficData: {},
  connections: {},
  historicalData: [],
  initialized: false
})

const sockets: Record<string, WebSocket> = {}

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
  }

  ws.onclose = () => {
    state.connections[road] = false
    if (!state.initialized) return
    setTimeout(() => {
      if (state.initialized && state.roads.includes(road)) connectRoad(road)
    }, 1500)
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

export function closeTrafficStore() {
  state.initialized = false
  Object.values(sockets).forEach((ws) => {
    try {
      ws.close()
    } catch {
      // ignore
    }
  })
  for (const k of Object.keys(sockets)) delete sockets[k]
  state.connections = {}
}

export function useTrafficStoreState() {
  return state
}
