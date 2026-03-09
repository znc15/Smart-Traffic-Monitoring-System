import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from './lib/api'

const LoginView = () => import('./views/LoginView.vue')
const DashboardView = () => import('./views/DashboardView.vue')
const AnalyticsView = () => import('./views/AnalyticsView.vue')
const AdminView = () => import('./views/AdminView.vue')
const ApiDocsView = () => import('./views/ApiDocsView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => (getToken() ? '/dashboard' : '/login') },
    { path: '/login', component: LoginView, meta: { title: '登录' } },
    { path: '/api-docs', component: ApiDocsView, meta: { title: 'API 文档' } },
    { path: '/dashboard', component: DashboardView, meta: { requiresAuth: true, title: '仪表盘' } },
    { path: '/analytics', component: AnalyticsView, meta: { requiresAuth: true, title: '数据分析' } },
    { path: '/admin', component: AdminView, meta: { requiresAuth: true, title: '系统管理' } }
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
