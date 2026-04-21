declare global {
  interface Window {
    AMap?: any
    __amapLoaderPromises__?: Partial<Record<string, Promise<any>>>
    _AMapSecurityConfig?: {
      securityJsCode?: string
      serviceHost?: string
    }
  }
}

export type EnsureAmapOptions = {
  key: string
  securityJsCode?: string | null
  serviceHost?: string | null
}

const getLoaderMap = () => {
  if (!window.__amapLoaderPromises__) {
    window.__amapLoaderPromises__ = {}
  }
  return window.__amapLoaderPromises__
}

function normalizeText(value: string | null | undefined): string {
  return String(value || '').trim()
}

function resolveSecurityConfig(options: EnsureAmapOptions) {
  const serviceHost = normalizeText(options.serviceHost)
  if (serviceHost) {
    return { serviceHost }
  }

  const securityJsCode = normalizeText(options.securityJsCode)
  if (securityJsCode) {
    return { securityJsCode }
  }

  return null
}

function getLoaderCacheKey(key: string, securityConfig: ReturnType<typeof resolveSecurityConfig>) {
  return JSON.stringify({
    key,
    serviceHost: securityConfig?.serviceHost || '',
    securityJsCode: securityConfig?.securityJsCode || '',
  })
}

function applySecurityConfig(securityConfig: ReturnType<typeof resolveSecurityConfig>) {
  if (securityConfig) {
    window._AMapSecurityConfig = { ...securityConfig }
    return
  }

  delete window._AMapSecurityConfig
}

export async function ensureAmap(options: EnsureAmapOptions) {
  const normalizedKey = normalizeText(options.key)
  if (!normalizedKey) {
    throw new Error('缺少 VITE_AMAP_KEY')
  }
  if (window.AMap) {
    return window.AMap
  }

  const securityConfig = resolveSecurityConfig(options)
  const loaderMap = getLoaderMap()
  const cacheKey = getLoaderCacheKey(normalizedKey, securityConfig)

  if (loaderMap[cacheKey]) {
    return loaderMap[cacheKey]
  }

  const promise = new Promise<any>((resolve, reject) => {
    const script = document.createElement('script')
    applySecurityConfig(securityConfig)
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(normalizedKey)}`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => {
      delete loaderMap[cacheKey]
      reject(new Error('高德地图脚本加载失败'))
    }
    document.head.appendChild(script)
  })

  loaderMap[cacheKey] = promise
  return promise
}
