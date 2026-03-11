import { afterEach, describe, expect, it, vi } from 'vitest'
import { ensureAmap } from './amap'

type MockScript = {
  src: string
  async: boolean
  onload: null | (() => void)
  onerror: null | (() => void)
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
        const windowMock = globalThis.window as Window & { AMap?: { version: string } }
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    const first = ensureAmap('demo key')
    const second = ensureAmap('demo key')
    const [firstResult, secondResult] = await Promise.all([first, second])

    expect(firstResult).toEqual({ version: '2.0' })
    expect(secondResult).toBe(firstResult)
    expect(scripts).toHaveLength(1)
    expect(scripts[0]?.src).toContain('key=demo%20key')
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
        const windowMock = globalThis.window as Window & { AMap?: { version: string } }
        windowMock.AMap = { version: '2.0' }
        script.onload?.()
      })
    })

    await expect(ensureAmap('retry-key')).rejects.toThrow('高德地图脚本加载失败')

    shouldFail = false
    const result = await ensureAmap('retry-key')

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

    await expect(ensureAmap('   ')).rejects.toThrow('缺少 VITE_AMAP_KEY')
    expect(appendChild).not.toHaveBeenCalled()
  })
})

function stubBrowser(onAppend: (script: MockScript) => void) {
  vi.stubGlobal('window', {})
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
}
