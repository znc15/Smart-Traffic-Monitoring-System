<template>
  <div class="layout">
    <header v-if="showHeader" class="topbar">
      <div class="brand">{{ siteName }}</div>
      <nav class="links">
        <RouterLink to="/dashboard">首页监控</RouterLink>
        <RouterLink to="/analytics">分析报表</RouterLink>
        <RouterLink to="/admin">管理员</RouterLink>
      </nav>
      <button class="logout" @click="logout">退出</button>
    </header>
    <main :class="showHeader ? 'with-header' : ''">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearToken, endpoints } from './lib/api'
import { normalizeSiteSettings } from './lib/normalize'
import { closeTrafficStore } from './store/traffic'

const route = useRoute()
const router = useRouter()
const siteName = ref('智能交通监控系统')

const showHeader = computed(() => route.path !== '/login')

const logout = () => {
  clearToken()
  closeTrafficStore()
  router.replace('/login')
}

onMounted(async () => {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) return
    const body = await res.json()
    siteName.value = normalizeSiteSettings(body).site_name
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #e2e8f0 0%, #f8fafc 100%);
  color: #0f172a;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 12px 20px;
  background: #0f172a;
  color: #f8fafc;
}

.brand {
  font-weight: 700;
}

.links {
  display: flex;
  gap: 12px;
  flex: 1;
}

.links a {
  color: #cbd5e1;
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 8px;
}

.links a.router-link-active {
  background: #0369a1;
  color: #fff;
}

.logout {
  border: 0;
  border-radius: 8px;
  padding: 8px 12px;
  background: #ef4444;
  color: #fff;
  cursor: pointer;
}

main {
  padding: 20px;
}

.with-header {
  padding-top: 16px;
}

@media (max-width: 768px) {
  .topbar {
    flex-wrap: wrap;
  }

  .links {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
