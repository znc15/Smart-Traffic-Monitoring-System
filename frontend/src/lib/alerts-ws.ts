type ReconnectPolicy = {
  max: number
  delayMs: number
}

type ConnectOptions = {
  url: string
  onAlert: (payload: unknown) => void
  onConnectionChange?: (connected: boolean) => void
  reconnect?: ReconnectPolicy
  WebSocketImpl?: typeof WebSocket
}

const DEFAULT_RECONNECT: ReconnectPolicy = { max: 10, delayMs: 1500 }

export function connectAlertsWs(options: ConnectOptions) {
  const WebSocketCtor = options.WebSocketImpl ?? WebSocket
  const policy = options.reconnect ?? DEFAULT_RECONNECT

  let attempts = 0
  let ws: WebSocket | null = null
  let closedManually = false
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const clearReconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  const connect = () => {
    clearReconnect()
    ws = new WebSocketCtor(options.url)

    ws.onopen = () => {
      attempts = 0
      options.onConnectionChange?.(true)
    }

    ws.onmessage = (ev) => {
      try {
        options.onAlert(JSON.parse(String(ev.data ?? '')))
      } catch {
        // ignore parse errors
      }
    }

    const scheduleReconnect = () => {
      options.onConnectionChange?.(false)
      if (closedManually) return
      if (attempts >= policy.max) return
      attempts += 1
      reconnectTimer = setTimeout(() => connect(), policy.delayMs)
    }

    ws.onclose = scheduleReconnect
    ws.onerror = scheduleReconnect
  }

  connect()

  return {
    close() {
      closedManually = true
      clearReconnect()
      options.onConnectionChange?.(false)
      if (ws) {
        try {
          ws.close()
        } catch {
          // ignore
        }
      }
      ws = null
    },
  }
}
