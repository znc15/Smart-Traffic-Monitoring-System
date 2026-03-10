import { AMAP_KEY } from './api'

declare global {
  interface Window {
    AMap?: any
    __amapLoaderPromise__?: Promise<any>
  }
}

export async function ensureAmap() {
  if (!AMAP_KEY) {
    throw new Error('缺少 VITE_AMAP_KEY')
  }
  if (window.AMap) {
    return window.AMap
  }
  if (!window.__amapLoaderPromise__) {
    window.__amapLoaderPromise__ = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(
        AMAP_KEY,
      )}&plugin=AMap.HeatMap,AMap.Scale,AMap.ToolBar`
      script.async = true
      script.onload = () => resolve(window.AMap)
      script.onerror = () => reject(new Error('高德地图脚本加载失败'))
      document.head.appendChild(script)
    })
  }
  return window.__amapLoaderPromise__
}
