import { afterEach, describe, expect, it, vi } from 'vitest'

type StorageMock = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
  clear: () => void
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

describe('lib/api', () => {
  afterEach(() => {
    vi.resetModules()
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('should resolve base urls from browser location and append websocket token', async () => {
    const localStorage = createLocalStorage()
    const replace = vi.fn()

    vi.stubGlobal('localStorage', localStorage)
    vi.stubGlobal('window', {
      location: {
        origin: 'https://smart.local',
        protocol: 'https:',
        host: 'smart.local',
        pathname: '/dashboard',
        replace,
      },
    })

    const api = await import('./api')
    api.setToken('jwt-token')

    expect(api.API_HTTP_BASE).toBe('https://smart.local/api/v1')
    expect(api.API_WS_BASE).toBe('wss://smart.local/api/v1')
    expect(api.getWsUrl('wss://smart.local/api/v1/ws/info/demo')).toContain(
      'token=jwt-token',
    )
  })

  it('should add auth header and redirect to login on 401', async () => {
    const localStorage = createLocalStorage()
    const replace = vi.fn()
    const fetchMock = vi.fn().mockResolvedValue({
      status: 401,
      ok: false,
    })

    vi.stubGlobal('localStorage', localStorage)
    vi.stubGlobal('window', {
      location: {
        origin: 'http://localhost:5174',
        protocol: 'http:',
        host: 'localhost:5174',
        pathname: '/dashboard',
        replace,
      },
    })
    vi.stubGlobal('fetch', fetchMock)

    const api = await import('./api')
    api.setToken('jwt-token')

    await expect(api.authFetch('/api/v1/auth/me')).rejects.toThrow('Unauthorized')

    const [, init] = fetchMock.mock.calls[0]
    const headers = new Headers(init.headers)

    expect(headers.get('Authorization')).toBe('Bearer jwt-token')
    expect(headers.get('Content-Type')).toBe('application/json')
    expect(init.credentials).toBe('include')
    expect(localStorage.getItem(api.TOKEN_KEY)).toBeNull()
    expect(replace).toHaveBeenCalledWith('/login')
  })
})
