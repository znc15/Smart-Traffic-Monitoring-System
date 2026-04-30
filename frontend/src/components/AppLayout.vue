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
        <template v-for="group in menuGroups" :key="group.label">
          <!-- Expanded: group header + collapsible sub-items -->
          <template v-if="!collapsed">
            <div
              class="flex items-center rounded-md px-3 py-2 text-sm font-medium cursor-pointer transition-colors hover:bg-accent hover:text-accent-foreground text-muted-foreground"
              @click="toggleGroup(group.label)"
            >
              <component :is="group.icon" class="h-5 w-5 shrink-0" />
              <span class="ml-3 flex-1 whitespace-nowrap">{{ group.label }}</span>
              <ChevronDown
                class="h-4 w-4 shrink-0 transition-transform duration-200"
                :class="isGroupExpanded(group.label) ? '' : '-rotate-90'"
              />
            </div>
            <div
              class="overflow-hidden transition-all duration-200 ease-in-out"
              :class="isGroupExpanded(group.label) ? 'max-h-96' : 'max-h-0'"
            >
              <RouterLink
                v-for="item in group.children"
                :key="item.key"
                :to="item.key"
                class="flex items-center rounded-md px-3 py-2 pl-11 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground"
                :class="[isActive(item.key) ? 'bg-accent text-accent-foreground' : 'text-muted-foreground']"
              >
                <component :is="item.icon" class="h-4 w-4 shrink-0" />
                <span class="ml-3 whitespace-nowrap">{{ item.label }}</span>
              </RouterLink>
            </div>
          </template>

          <!-- Collapsed: single child = direct link, multi = group icon with flyout -->
          <template v-else>
            <RouterLink
              v-if="group.children.length === 1"
              :to="group.children[0].key"
              class="flex items-center justify-center rounded-md p-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground mx-1"
              :class="[isActive(group.children[0].key) ? 'bg-accent text-accent-foreground' : 'text-muted-foreground']"
              :title="group.children[0].label"
            >
              <component :is="group.children[0].icon" class="h-5 w-5 shrink-0" />
            </RouterLink>

            <div
              v-else
              class="relative"
              @mouseenter="hoveredGroup = group.label"
              @mouseleave="hoveredGroup = null"
            >
              <div
                class="flex items-center justify-center rounded-md p-2 text-sm font-medium cursor-pointer transition-colors hover:bg-accent hover:text-accent-foreground text-muted-foreground mx-1"
                :class="{ 'bg-accent/50 text-accent-foreground': isGroupActive(group) }"
                :title="group.label"
              >
                <component :is="group.icon" class="h-5 w-5 shrink-0" />
              </div>
              <Transition name="flyout">
                <div
                  v-if="hoveredGroup === group.label"
                  class="absolute left-full top-0 ml-2 z-50 bg-card border border-border rounded-md shadow-lg py-1 min-w-[160px]"
                >
                  <div class="px-3 py-1.5 text-xs font-semibold text-muted-foreground">{{ group.label }}</div>
                  <RouterLink
                    v-for="item in group.children"
                    :key="item.key"
                    :to="item.key"
                    class="flex items-center px-3 py-2 text-sm transition-colors hover:bg-accent hover:text-accent-foreground"
                    :class="[isActive(item.key) ? 'bg-accent text-accent-foreground' : 'text-muted-foreground']"
                  >
                    <component :is="item.icon" class="h-4 w-4 shrink-0" />
                    <span class="ml-2 whitespace-nowrap">{{ item.label }}</span>
                  </RouterLink>
                </div>
              </Transition>
            </div>
          </template>
        </template>
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

    <!-- AI 助手悬浮窗 -->
    <AiFloatButton />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
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
  ChevronDown,
  Sun,
  Moon,
  Bot,
  Activity,
  Video,
  BellRing,
  Globe,
  Users,
  KeyRound,
  FileText,
} from 'lucide-vue-next'
import { clearToken, endpoints, authFetch } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { normalizeRoleId, isAdminRole } from '../lib/roles'
import { closeTrafficStore } from '../store/traffic'
import { Button } from '@/components/ui/button'
import { Popover, PopoverTrigger, PopoverContent } from '@/components/ui/popover'
import AiFloatButton from './AiFloatButton.vue'

interface MenuItem {
  label: string
  key: string
  icon: any
}

interface MenuGroup {
  label: string
  icon: any
  children: MenuItem[]
}

const isDark = useDark()
const toggleDark = useToggle(isDark)

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const username = ref('用户')
const siteName = ref('智慧交通监控')
const footerText = ref('')
const hoveredGroup = ref<string | null>(null)
const expandedGroups = ref<Set<string>>(new Set())
const roleId = ref<number | null>(null)

const MOBILE_BREAKPOINT = 768

const allMenuGroups: MenuGroup[] = [
  {
    label: '监控中心',
    icon: Activity,
    children: [
      { label: '交通态势概览', key: '/dashboard', icon: LayoutDashboard },
      { label: '实时视频监测', key: '/cameras', icon: Video },
      { label: '节点监控汇聚', key: '/monitoring', icon: Activity },
      { label: '实时状态推送', key: '/map', icon: Map },
    ],
  },
  {
    label: '数据分析',
    icon: BarChart,
    children: [
      { label: '历史数据统计', key: '/analytics', icon: BarChart },
      { label: '异常事件告警', key: '/alerts', icon: BellRing },
    ],
  },
  {
    label: '系统管理',
    icon: Settings,
    children: [
      { label: '站点设置', key: '/admin/site', icon: Globe },
      { label: 'AI 配置', key: '/admin/ai', icon: Bot },
      { label: '用户管理', key: '/admin/users', icon: Users },
      { label: 'API 密钥', key: '/admin/keys', icon: KeyRound },
      { label: 'API 文档', key: '/admin/docs', icon: FileText },
    ],
  },
  {
    label: 'AI 助手',
    icon: Bot,
    children: [
      { label: 'AI 智能分析', key: '/ai-assistant', icon: Bot },
    ],
  },
]

const menuGroups = computed<MenuGroup[]>(() => {
  if (isAdminRole(roleId.value)) {
    return allMenuGroups
  }
  return allMenuGroups.filter((group) => group.label !== '系统管理')
})

function isActive(key: string): boolean {
  if (route.path === key) return true
  // /admin loads AdminView with default site tab
  if (key === '/admin/site' && route.path === '/admin') return true
  return false
}

function isGroupActive(group: MenuGroup): boolean {
  return group.children.some(child => isActive(child.key))
}

function isGroupExpanded(label: string): boolean {
  return expandedGroups.value.has(label)
}

function toggleGroup(label: string) {
  if (expandedGroups.value.has(label)) {
    expandedGroups.value.delete(label)
  } else {
    expandedGroups.value.add(label)
  }
}

// Auto-expand group containing active route
watch(() => route.path, () => {
  for (const group of menuGroups.value) {
    if (isGroupActive(group)) {
      expandedGroups.value.add(group.label)
    }
  }
}, { immediate: true })

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
    roleId.value = normalizeRoleId(data?.role_id ?? data?.roleId)
    if (!isAdminRole(roleId.value) && route.path.startsWith('/admin')) {
      router.replace('/dashboard')
    }
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

const pageTitle = computed(() => {
  return (route.meta?.title as string) || '智慧交通监控'
})

function handleLogout() {
  clearToken()
  closeTrafficStore()
  roleId.value = null
  router.replace('/login')
}
</script>

<style scoped>
.flyout-enter-active,
.flyout-leave-active {
  transition: opacity 150ms ease, transform 150ms ease;
}
.flyout-enter-from,
.flyout-leave-to {
  opacity: 0;
  transform: translateX(-4px);
}
</style>
