import { createRouter, createWebHistory } from 'vue-router'
import { authFetch, endpoints, getToken } from './lib/api'
import { isAdminRole } from './lib/roles'

const LoginView = () => import('./views/LoginView.vue')
const HomeView = () => import('./views/HomeView.vue')
const DashboardView = () => import('./views/DashboardView.vue')
const MapView = () => import('./views/MapView.vue')
const AnalyticsView = () => import('./views/AnalyticsView.vue')
const MonitoringView = () => import('./views/MonitoringView.vue')
const CamerasView = () => import('./views/CamerasView.vue')
const SiteSettingsView = () => import('./views/admin/SiteSettingsView.vue')
const AiConfigView = () => import('./views/admin/AiConfigView.vue')
const ApiDocsView = () => import('./views/admin/ApiDocsView.vue')
const AlertsView = () => import('./views/AlertsView.vue')
const AiAssistantView = () => import('./views/AiAssistantView.vue')
const UserManagementView = () => import('./views/admin/UserManagementView.vue')
const ApiKeyManagementView = () => import('./views/admin/ApiKeyManagementView.vue')

const authOnly = { requiresAuth: true }
const adminOnly = { requiresAuth: true, requiresAdmin: true }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView, meta: { title: '首页', noLayout: true } },
    { path: '/login', component: LoginView, meta: { title: '登录', noLayout: true } },
    { path: '/dashboard', component: DashboardView, meta: { ...authOnly, title: '交通态势概览' } },
    { path: '/cameras', component: CamerasView, meta: { ...authOnly, title: '实时视频监测' } },
    { path: '/monitoring', component: MonitoringView, meta: { ...authOnly, title: '节点监控汇聚' } },
    { path: '/map', component: MapView, meta: { ...authOnly, title: '实时状态推送' } },
    { path: '/analytics', component: AnalyticsView, meta: { ...authOnly, title: '历史数据统计' } },
    { path: '/alerts', component: AlertsView, meta: { ...authOnly, title: '异常事件告警' } },
    // Admin sub-routes
    { path: '/admin', component: SiteSettingsView, meta: { ...adminOnly, title: '站点设置' } },
    { path: '/admin/site', component: SiteSettingsView, meta: { ...adminOnly, title: '站点设置' } },
    { path: '/admin/ai', component: AiConfigView, meta: { ...adminOnly, title: 'AI 配置' } },
    { path: '/admin/users', component: UserManagementView, meta: { ...adminOnly, title: '用户管理' } },
    { path: '/admin/keys', component: ApiKeyManagementView, meta: { ...adminOnly, title: 'API 密钥' } },
    { path: '/admin/docs', component: ApiDocsView, meta: { ...adminOnly, title: 'API 文档' } },
    { path: '/ai-assistant', component: AiAssistantView, meta: { ...authOnly, title: 'AI 智能分析' } },
  ]
})

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return '/login'
  }
  if (to.path === '/login' && getToken()) {
    return '/dashboard'
  }
  if (to.meta.requiresAdmin) {
    try {
      const res = await authFetch(endpoints.me)
      if (!res.ok) {
        return '/dashboard'
      }
      const data = await res.json()
      if (!isAdminRole(data?.role_id ?? data?.roleId)) {
        return '/dashboard'
      }
    } catch {
      return false
    }
  }
  return true
})

export default router
