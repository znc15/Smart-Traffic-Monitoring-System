import { afterEach, describe, expect, it, vi } from 'vitest'
import { ensureAmap } from './amap'

type MockScript = {
  src: string
  async: boolean
  onload: null | (() => void)
  onerror: null | (() => void)
}

type MockWindow = Window & {
  AMap?: { version: string }
  _AMapSecurityConfig?: {
    securityJsCode?: string
    serviceHost?: string
  }
}

describe('lib/amap', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('should reuse the same pending loader for the same key', async () => {
    const scripts: MockScript[] = []
    stubBrowser((script) => {
      scripts.push(script)
      queueMicrotask(() => {
        const windowMock = globalThis.window as MockWindow
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    const first = ensureAmap({ key: 'demo key', securityJsCode: 'secure-code' })
    const second = ensureAmap({ key: 'demo key', securityJsCode: 'secure-code' })
    const [firstResult, secondResult] = await Promise.all([first, second])

    expect(firstResult).toEqual({ version: '2.0' })
    expect(secondResult).toBe(firstResult)
    expect(scripts).toHaveLength(1)
    expect(scripts[0]?.src).toContain('key=demo%20key')
    expect(scripts[0]?.src).not.toContain('plugin=')
  })

  it('should allow retrying the same key after a failed script load', async () => {
    const scripts: MockScript[] = []
    let shouldFail = true
    stubBrowser((script) => {
      scripts.push(script)
      queueMicrotask(() => {
        if (shouldFail) {
          script.onerror?.()
          return
        }
        const windowMock = globalThis.window as MockWindow
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    await expect(ensureAmap({ key: 'retry-key' })).rejects.toThrow('高德地图脚本加载失败')

    shouldFail = false
    const result = await ensureAmap({ key: 'retry-key' })

    expect(result).toEqual({ version: '2.0' })
    expect(scripts).toHaveLength(2)
    expect(scripts[1]?.src).toContain('key=retry-key')
  })

  it('should reject blank keys before touching the DOM', async () => {
    const appendChild = vi.fn()
    vi.stubGlobal('window', {})
    vi.stubGlobal('document', {
      createElement: vi.fn(),
      head: { appendChild },
    })

    await expect(ensureAmap({ key: '   ' })).rejects.toThrow('缺少 VITE_AMAP_KEY')
    expect(appendChild).not.toHaveBeenCalled()
  })

  it('should isolate pending loaders by effective security config', async () => {
    const scripts: MockScript[] = []
    const appliedConfigs: Array<MockWindow['_AMapSecurityConfig'] | undefined> = []

    stubBrowser((script) => {
      scripts.push(script)
      const windowMock = globalThis.window as MockWindow
      appliedConfigs.push(windowMock._AMapSecurityConfig ? { ...windowMock._AMapSecurityConfig } : undefined)
      queueMicrotask(() => {
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    const first = ensureAmap({ key: 'shared-key', securityJsCode: 'code-a' })
    const second = ensureAmap({ key: 'shared-key', securityJsCode: 'code-b' })
    await Promise.all([first, second])

    expect(scripts).toHaveLength(2)
    expect(appliedConfigs).toEqual([{ securityJsCode: 'code-a' }, { securityJsCode: 'code-b' }])
  })

  it('should apply securityJsCode before appending the script', async () => {
    const appliedConfigs: Array<MockWindow['_AMapSecurityConfig'] | undefined> = []
    const scripts: MockScript[] = []

    stubBrowser((script) => {
      scripts.push(script)
      const windowMock = globalThis.window as MockWindow
      appliedConfigs.push(windowMock._AMapSecurityConfig ? { ...windowMock._AMapSecurityConfig } : undefined)
      queueMicrotask(() => {
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    await ensureAmap({ key: 'secure-key', securityJsCode: 'security-code' })

    expect(appliedConfigs).toEqual([{ securityJsCode: 'security-code' }])
    expect(scripts[0]?.src).toContain('key=secure-key')
    expect(scripts[0]?.src).not.toContain('plugin=')
  })

  it('should prefer serviceHost over securityJsCode when both are provided', async () => {
    const appliedConfigs: Array<MockWindow['_AMapSecurityConfig'] | undefined> = []

    stubBrowser((script) => {
      const windowMock = globalThis.window as MockWindow
      appliedConfigs.push(windowMock._AMapSecurityConfig ? { ...windowMock._AMapSecurityConfig } : undefined)
      queueMicrotask(() => {
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    await ensureAmap({
      key: 'proxy-key',
      securityJsCode: 'ignored-security-code',
      serviceHost: '/_AMapService',
    })

    expect(appliedConfigs).toEqual([{ serviceHost: '/_AMapService' }])
  })
})

function stubBrowser(onAppend: (script: MockScript) => void) {
  const windowMock = {} as MockWindow
  vi.stubGlobal('window', windowMock)
  vi.stubGlobal('document', {
    createElement: vi.fn((): MockScript => ({
      src: '',
      async: false,
      onload: null,
      onerror: null,
    })),
    head: {
      appendChild: vi.fn((script: MockScript) => {
        onAppend(script)
        return script
      }),
    },
  })

  return windowMock
}
