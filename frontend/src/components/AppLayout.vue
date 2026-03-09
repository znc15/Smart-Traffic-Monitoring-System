<template>
  <n-layout has-sider position="absolute" class="app-layout">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
      :native-scrollbar="false"
      class="app-sider"
    >
      <div class="sider-header" :class="{ 'sider-header--collapsed': collapsed }">
        <n-icon size="28" color="#2080f0">
          <CarOutline />
        </n-icon>
        <transition name="fade">
          <span v-if="!collapsed" class="sider-title">{{ siteName }}</span>
        </transition>
      </div>

      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuUpdate"
      />

      <div class="sider-footer" v-if="!collapsed">
        <n-text depth="3" style="font-size: 12px">{{ footerText || 'v1.0.0' }}</n-text>
      </div>
    </n-layout-sider>

    <n-layout class="app-inner-layout">
      <n-layout-header bordered class="app-header">
        <div class="header-left">
          <n-text strong style="font-size: 16px">{{ pageTitle }}</n-text>
        </div>
        <div class="header-right">
          <n-dropdown :options="userDropdownOptions" @select="handleUserAction">
            <n-button quaternary>
              <template #icon>
                <n-icon><PersonOutline /></n-icon>
              </template>
              {{ username }}
            </n-button>
          </n-dropdown>
        </div>
      </n-layout-header>

      <n-layout-content class="app-content" :native-scrollbar="false">
        <slot />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onUnmounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon } from 'naive-ui'
import {
  SpeedometerOutline,
  BarChartOutline,
  SettingsOutline,
  CarOutline,
  PersonOutline,
  LogOutOutline,
  CodeSlashOutline,
} from '@vicons/ionicons5'
import { clearToken, endpoints, authFetch } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { closeTrafficStore } from '../store/traffic'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const username = ref('用户')
const siteName = ref('智慧交通监控')
const footerText = ref('')

// Responsive: auto-collapse on mobile
const MOBILE_BREAKPOINT = 768

function checkMobile() {
  collapsed.value = window.innerWidth < MOBILE_BREAKPOINT
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  fetchUserInfo()
  fetchSiteSettings()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

async function fetchUserInfo() {
  try {
    const res = await authFetch(endpoints.me)
    if (!res.ok) return
    const data = await res.json()
    username.value = data.username || '用户'
  } catch {
    // silently ignore - non-critical UI enrichment
  }
}

async function fetchSiteSettings() {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) return
    const body = await res.json()
    const settings = normalizeSiteSettings(body)
    siteName.value = settings.site_name || '智慧交通监控'
    footerText.value = settings.footer_text
  } catch {
    // silently ignore - non-critical UI enrichment
  }
}

// Menu rendering helper
function renderIcon(icon: Component) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions = [
  {
    label: '仪表盘',
    key: '/dashboard',
    icon: renderIcon(SpeedometerOutline),
  },
  {
    label: '数据分析',
    key: '/analytics',
    icon: renderIcon(BarChartOutline),
  },
  {
    label: '系统管理',
    key: '/admin',
    icon: renderIcon(SettingsOutline),
  },
  {
    label: 'API 文档',
    key: '/api-docs',
    icon: renderIcon(CodeSlashOutline),
  },
]

const activeKey = computed(() => route.path)

const pageTitleMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/analytics': '数据分析',
  '/admin': '系统管理',
  '/api-docs': 'API 文档',
}

const pageTitle = computed(() => {
  return (route.meta?.title as string) || pageTitleMap[route.path] || '智慧交通监控'
})

function handleMenuUpdate(key: string) {
  router.push(key)
}

// User dropdown
const userDropdownOptions = [
  {
    label: '退出登录',
    key: 'logout',
    icon: renderIcon(LogOutOutline),
  },
]

function handleUserAction(key: string) {
  if (key === 'logout') {
    clearToken()
    closeTrafficStore()
    router.replace('/login')
  }
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-sider {
  background: #fff;
}

.sider-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  min-height: 64px;
  transition: padding 0.3s ease;
}

.sider-header--collapsed {
  padding: 20px 18px;
  justify-content: center;
}

.sider-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
}

.sider-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.app-inner-layout {
  background: #f8fafc;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.app-content {
  padding: 24px;
  background: #f8fafc;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
