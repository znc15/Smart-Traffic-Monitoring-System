import { afterEach, describe, expect, it, vi } from 'vitest'

type StorageMock = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
  clear: () => void
}

class FakeWebSocket {
  static instances: FakeWebSocket[] = []

  url: string
  onopen: (() => void) | null = null
  onclose: (() => void) | null = null
  onerror: (() => void) | null = null
  onmessage: ((event: { data: string }) => void) | null = null
  close = vi.fn()

  constructor(url: string) {
    this.url = url
    FakeWebSocket.instances.push(this)
  }

  emitOpen() {
    this.onopen?.()
  }

  emitClose() {
    this.onclose?.()
  }

  emitMessage(payload: unknown) {
    this.onmessage?.({ data: JSON.stringify(payload) })
  }
}

function createLocalStorage(): StorageMock {
  const store = new Map<string, string>()
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => store.set(key, value),
    removeItem: (key) => store.delete(key),
    clear: () => store.clear(),
  }
}

describe('store/traffic', () => {
  afterEach(async () => {
    vi.useRealTimers()
    vi.resetModules()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    FakeWebSocket.instances = []
  })

  it('should initialize roads, open websockets and update traffic state', async () => {
    const localStorage = createLocalStorage()
    localStorage.setItem('access_token', 'edge-token')
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ road_names: ['人民路', '中山路'] }),
    })

    vi.stubGlobal('localStorage', localStorage)
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('WebSocket', FakeWebSocket as unknown as typeof WebSocket)

    const store = await import('./traffic')
    await store.initializeTrafficStore()

    expect(FakeWebSocket.instances).toHaveLength(2)
    expect(FakeWebSocket.instances[0].url).toContain('token=edge-token')

    FakeWebSocket.instances[0].emitOpen()
    FakeWebSocket.instances[0].emitMessage({
      count_car: 4,
      count_motor: 2,
      count_person: 1,
      speed_car: 28,
      speed_motor: 16,
      density_status: 'busy',
      speed_status: 'slow',
      online: true,
    })

    const state = store.useTrafficStoreState()
    expect(state.roads).toEqual(['人民路', '中山路'])
    expect(state.connections['人民路']).toBe(true)
    expect(state.trafficData['人民路'].count_car).toBe(4)
    expect(state.historicalData).toHaveLength(1)

    store.closeTrafficStore()
  })

  it('should reconnect closed roads and drop removed roads on refresh', async () => {
    vi.useFakeTimers()
    const localStorage = createLocalStorage()
    localStorage.setItem('access_token', 'edge-token')
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ road_names: ['人民路'] }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ road_names: ['中山路'] }),
      })

    vi.stubGlobal('localStorage', localStorage)
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('WebSocket', FakeWebSocket as unknown as typeof WebSocket)

    const store = await import('./traffic')
    await store.initializeTrafficStore()

    const initialSocket = FakeWebSocket.instances[0]
    initialSocket.emitOpen()
    initialSocket.emitClose()

    vi.advanceTimersByTime(1500)
    expect(FakeWebSocket.instances).toHaveLength(2)
    const reconnectedSocket = FakeWebSocket.instances[1]

    await store.refreshRoads()

    const state = store.useTrafficStoreState()
    expect(state.roads).toEqual(['中山路'])
    expect(reconnectedSocket.close).toHaveBeenCalled()

    store.closeTrafficStore()
  })
})
