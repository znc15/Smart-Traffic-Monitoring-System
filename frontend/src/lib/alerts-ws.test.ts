import { describe, expect, it, vi } from 'vitest'

class FakeWebSocket {
  static instances: FakeWebSocket[] = []

  onopen: ((ev: Event) => void) | null = null
  onclose: ((ev: CloseEvent) => void) | null = null
  onerror: ((ev: Event) => void) | null = null
  onmessage: ((ev: MessageEvent) => void) | null = null

  readyState = 0
  url: string

  constructor(url: string) {
    this.url = url
    FakeWebSocket.instances.push(this)
  }

  open() {
    this.readyState = 1
    this.onopen?.(new Event('open'))
  }

  message(data: string) {
    this.onmessage?.({ data } as MessageEvent)
  }

  close() {
    this.readyState = 3
    this.onclose?.({} as CloseEvent)
  }
}

describe('lib/alerts-ws', () => {
  it('should parse websocket message and call onAlert', async () => {
    const onAlert = vi.fn()
    const { connectAlertsWs } = await import('./alerts-ws')

    connectAlertsWs({
      url: 'ws://localhost/api/v1/admin/ws/alerts',
      onAlert,
      WebSocketImpl: FakeWebSocket as unknown as typeof WebSocket,
      reconnect: { max: 0, delayMs: 10 },
    })

    expect(FakeWebSocket.instances).toHaveLength(1)
    FakeWebSocket.instances[0].open()
    FakeWebSocket.instances[0].message(JSON.stringify({ id: 1, type: 'CONGESTION' }))

    expect(onAlert).toHaveBeenCalledWith({ id: 1, type: 'CONGESTION' })
  })

  it('should reconnect when connection closes unexpectedly', async () => {
    vi.useFakeTimers()
    const onAlert = vi.fn()
    const { connectAlertsWs } = await import('./alerts-ws')

    connectAlertsWs({
      url: 'ws://localhost/api/v1/admin/ws/alerts',
      onAlert,
      WebSocketImpl: FakeWebSocket as unknown as typeof WebSocket,
      reconnect: { max: 1, delayMs: 10 },
    })

    expect(FakeWebSocket.instances).toHaveLength(1)
    FakeWebSocket.instances[0].open()
    FakeWebSocket.instances[0].close()

    await vi.advanceTimersByTimeAsync(10)

    expect(FakeWebSocket.instances).toHaveLength(2)
    vi.useRealTimers()
  })
})

