declare global {
  interface Window {
    AMap?: any
    __amapLoaderPromises__?: Partial<Record<string, Promise<any>>>
  }
}

const getLoaderMap = () => {
  if (!window.__amapLoaderPromises__) {
    window.__amapLoaderPromises__ = {}
  }
  return window.__amapLoaderPromises__
}

export async function ensureAmap(key: string) {
  const normalizedKey = String(key || '').trim()
  if (!normalizedKey) {
    throw new Error('缺少 VITE_AMAP_KEY')
  }
  if (window.AMap) {
    return window.AMap
  }
  const loaderMap = getLoaderMap()
  if (loaderMap[normalizedKey]) {
    return loaderMap[normalizedKey]
  }
  const promise = new Promise<any>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(
      normalizedKey,
    )}&plugin=AMap.HeatMap,AMap.Scale,AMap.ToolBar`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => {
      delete loaderMap[normalizedKey]
      reject(new Error('高德地图脚本加载失败'))
    }
    document.head.appendChild(script)
  })
  loaderMap[normalizedKey] = promise
  return promise
}
