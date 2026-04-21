<template>
  <div class="flex h-screen w-full bg-background text-foreground overflow-hidden">
    <!-- Sidebar -->
    <aside
      class="border-r border-border bg-card transition-all duration-300 ease-in-out flex flex-col"
      :class="collapsed ? 'w-16' : 'w-64'"
    >
      <div class="h-16 flex items-center justify-center border-b border-border">
        <Car class="h-6 w-6 text-primary" />
        <span v-if="!collapsed" class="ml-3 font-bold text-lg whitespace-nowrap overflow-hidden">{{ siteName }}</span>
      </div>

      <nav class="flex-1 py-4 space-y-1 overflow-y-auto overflow-x-hidden px-2">
        <RouterLink
          v-for="item in menuOptions"
          :key="item.key"
          :to="item.key"
          class="flex items-center rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
          :class="[route.path === item.key ? 'bg-accent text-accent-foreground' : 'text-muted-foreground']"
          :title="collapsed ? item.label : ''"
        >
          <component :is="item.icon" class="h-5 w-5 shrink-0" />
          <span v-if="!collapsed" class="ml-3 whitespace-nowrap">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="p-4 border-t border-border flex items-center justify-center">
        <Button variant="ghost" size="icon" @click="collapsed = !collapsed" class="w-full">
          <Menu class="h-5 w-5" v-if="collapsed" />
          <ChevronLeft class="h-5 w-5" v-else />
        </Button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col min-w-0 overflow-hidden bg-background">
      <!-- Header -->
      <header class="h-16 border-b border-border bg-card/50 backdrop-blur flex items-center justify-between px-6 shrink-0 z-10">
        <h2 class="text-lg font-semibold">{{ pageTitle }}</h2>
        <div class="flex items-center space-x-4">
          <Button variant="ghost" size="icon" @click="toggleDark()">
            <Sun v-if="isDark" class="h-5 w-5" />
            <Moon v-else class="h-5 w-5" />
          </Button>

          <Popover>
            <PopoverTrigger as-child>
              <Button variant="ghost" class="flex items-center space-x-2">
                <User class="h-4 w-4" />
                <span class="text-sm">{{ username }}</span>
              </Button>
            </PopoverTrigger>
            <PopoverContent class="w-40 p-1" align="end">
              <Button variant="ghost" class="w-full justify-start text-destructive hover:text-destructive" @click="handleLogout">
                <LogOut class="h-4 w-4 mr-2" />
                退出登录
              </Button>
            </PopoverContent>
          </Popover>
        </div>
      </header>

      <!-- Page Content -->
      <div class="flex-1 overflow-auto p-6 relative">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDark, useToggle } from '@vueuse/core'
import {
  LayoutDashboard,
  Map,
  BarChart,
  Settings,
  Car,
  User,
  LogOut,
  Menu,
  ChevronLeft,
  Sun,
  Moon,
  Code
} from 'lucide-vue-next'
import { clearToken, endpoints, authFetch } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { closeTrafficStore } from '../store/traffic'
import { Button } from '@/components/ui/button'
import { Popover, PopoverTrigger, PopoverContent } from '@/components/ui/popover'

const isDark = useDark()
const toggleDark = useToggle(isDark)

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const username = ref('用户')
const siteName = ref('智慧交通监控')
const footerText = ref('')

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
  } catch {}
}

async function fetchSiteSettings() {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) return
    const body = await res.json()
    const settings = normalizeSiteSettings(body)
    siteName.value = settings.site_name || '智慧交通监控'
    footerText.value = settings.footer_text
  } catch {}
}

const menuOptions = [
  { label: '仪表盘', key: '/dashboard', icon: LayoutDashboard },
  { label: 'GIS 地图', key: '/map', icon: Map },
  { label: '数据分析', key: '/analytics', icon: BarChart },
  { label: '系统管理', key: '/admin', icon: Settings },
  { label: '开发者中心', key: '/developer', icon: Code },
]

const pageTitleMap: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/map': 'GIS 地图',
  '/analytics': '数据分析',
  '/admin': '系统管理',
  '/developer': '开发者中心',
}

const pageTitle = computed(() => {
  return (route.meta?.title as string) || pageTitleMap[route.path] || '智慧交通监控'
})

function handleLogout() {
  clearToken()
  closeTrafficStore()
  router.replace('/login')
}
</script>
