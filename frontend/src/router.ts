import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from './lib/api'

const LoginView = () => import('./views/LoginView.vue')
const HomeView = () => import('./views/HomeView.vue')
const DashboardView = () => import('./views/DashboardView.vue')
const MapView = () => import('./views/MapView.vue')
const AnalyticsView = () => import('./views/AnalyticsView.vue')
const MonitoringView = () => import('./views/MonitoringView.vue')
const CamerasView = () => import('./views/CamerasView.vue')
const AdminView = () => import('./views/AdminView.vue')
const AlertsView = () => import('./views/AlertsView.vue')
const AiAssistantView = () => import('./views/AiAssistantView.vue')
const UserManagementView = () => import('./views/admin/UserManagementView.vue')
const ApiKeyManagementView = () => import('./views/admin/ApiKeyManagementView.vue')

const adminAuth = { requiresAuth: true }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView, meta: { title: '首页', noLayout: true } },
    { path: '/login', component: LoginView, meta: { title: '登录', noLayout: true } },
    { path: '/dashboard', component: DashboardView, meta: { ...adminAuth, title: '交通态势概览' } },
    { path: '/cameras', component: CamerasView, meta: { ...adminAuth, title: '实时视频监测' } },
    { path: '/monitoring', component: MonitoringView, meta: { ...adminAuth, title: '节点监控汇聚' } },
    { path: '/map', component: MapView, meta: { ...adminAuth, title: '实时状态推送' } },
    { path: '/analytics', component: AnalyticsView, meta: { ...adminAuth, title: '历史数据统计' } },
    { path: '/alerts', component: AlertsView, meta: { ...adminAuth, title: '异常事件告警' } },
    // Admin sub-routes
    { path: '/admin', component: AdminView, meta: { ...adminAuth, title: '站点设置' } },
    { path: '/admin/site', component: AdminView, meta: { ...adminAuth, title: '站点设置' } },
    { path: '/admin/ai', component: AdminView, meta: { ...adminAuth, title: 'AI 配置' } },
    { path: '/admin/users', component: UserManagementView, meta: { ...adminAuth, title: '用户管理' } },
    { path: '/admin/keys', component: ApiKeyManagementView, meta: { ...adminAuth, title: 'API 密钥' } },
    { path: '/admin/docs', component: AdminView, meta: { ...adminAuth, title: 'API 文档' } },
    { path: '/ai-assistant', component: AiAssistantView, meta: { ...adminAuth, title: 'AI 智能分析' } },
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return '/login'
  }
  if (to.path === '/login' && getToken()) {
    return '/dashboard'
  }
  return true
})

export default router