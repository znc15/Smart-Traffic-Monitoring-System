import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import DashboardView from './views/DashboardView.vue'
import AnalyticsView from './views/AnalyticsView.vue'
import AdminView from './views/AdminView.vue'
import { getToken } from './lib/api'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: () => (getToken() ? '/dashboard' : '/login') },
    { path: '/login', component: LoginView, meta: { title: '登录' } },
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
