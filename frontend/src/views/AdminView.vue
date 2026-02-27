<template>
  <section class="admin">
    <div v-if="loading">加载中...</div>
    <div v-else-if="errorText" class="error">{{ errorText }}</div>
    <template v-else>
      <div class="tabs">
        <button :class="{ active: tab === 'users' }" @click="tab = 'users'">用户管理</button>
        <button :class="{ active: tab === 'cameras' }" @click="tab = 'cameras'">摄像头管理</button>
        <button :class="{ active: tab === 'settings' }" @click="tab = 'settings'">站点设置</button>
        <button :class="{ active: tab === 'monitor' }" @click="tab = 'monitor'">系统监控</button>
      </div>

      <div v-if="tab === 'users'" class="card">
        <h3>用户列表</h3>
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td>{{ u.username }}</td>
              <td>{{ u.email }}</td>
              <td>{{ u.role_id === 0 ? '管理员' : '用户' }}</td>
              <td>{{ u.enabled ? '启用' : '禁用' }}</td>
              <td>
                <button class="secondary" @click="toggleRole(u)">切换角色</button>
                <button class="danger" @click="toggleStatus(u)">切换状态</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="tab === 'cameras'" class="card">
        <h3>摄像头管理</h3>
        <div class="form-row">
          <input v-model="cameraForm.name" placeholder="名称" />
          <input v-model="cameraForm.road_name" placeholder="路段" />
          <input v-model="cameraForm.location" placeholder="位置" />
          <input v-model="cameraForm.stream_url" placeholder="stream_url" />
          <button @click="createCamera">新增</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>路段</th>
              <th>地址</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in cameras" :key="c.id">
              <td>{{ c.name }}</td>
              <td>{{ c.road_name }}</td>
              <td>{{ c.stream_url || '-' }}</td>
              <td>{{ c.enabled ? '启用' : '禁用' }}</td>
              <td>
                <button class="secondary" @click="editCamera(c)">编辑</button>
                <button class="secondary" @click="toggleCamera(c)">切换状态</button>
                <button class="danger" @click="deleteCamera(c.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="tab === 'settings'" class="card">
        <h3>站点设置</h3>
        <div class="settings-grid">
          <label>
            site_name
            <input v-model="settings.site_name" />
          </label>
          <label>
            logo_url
            <input v-model="settings.logo_url" />
          </label>
          <label>
            announcement
            <textarea v-model="settings.announcement" rows="4"></textarea>
          </label>
          <label>
            footer_text
            <input v-model="settings.footer_text" />
          </label>
        </div>
        <button @click="saveSettings">保存设置</button>
      </div>

      <div v-if="tab === 'monitor'" class="card">
        <h3>系统监控</h3>
        <div class="monitor-grid">
          <div>
            <h4>资源指标</h4>
            <pre>{{ pretty(resources) }}</pre>
          </div>
          <div>
            <h4>节点状态</h4>
            <pre>{{ pretty(nodes) }}</pre>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { authFetch, endpoints } from '../lib/api'
import {
  normalizeAdminUser,
  normalizeCamera,
  normalizeSiteSettings,
  type AdminUser,
  type CameraItem,
  type SiteSettings
} from '../lib/normalize'

const tab = ref<'users' | 'cameras' | 'settings' | 'monitor'>('users')
const loading = ref(true)
const errorText = ref('')

const users = ref<AdminUser[]>([])
const cameras = ref<CameraItem[]>([])
const settings = reactive<SiteSettings>({
  site_name: '',
  announcement: '',
  logo_url: '',
  footer_text: ''
})
const cameraForm = reactive({
  name: '',
  road_name: '',
  location: '',
  stream_url: ''
})

const resources = ref<Record<string, unknown>>({})
const nodes = ref<Record<string, unknown>>({})
let monitorTimer: number | null = null

const pretty = (obj: unknown) => JSON.stringify(obj, null, 2)

const ensureAdmin = async () => {
  const me = await authFetch(endpoints.me)
  if (!me.ok) throw new Error('无法验证登录状态')
  const data = await me.json()
  const role = Number(data?.role_id ?? data?.roleId ?? 1)
  if (role !== 0) throw new Error('仅管理员可访问')
}

const fetchUsers = async () => {
  const res = await authFetch(endpoints.adminUsers)
  if (!res.ok) return
  const data = await res.json()
  users.value = Array.isArray(data) ? data.map(normalizeAdminUser) : []
}

const fetchCameras = async () => {
  const res = await authFetch(endpoints.adminCameras)
  if (!res.ok) return
  const data = await res.json()
  cameras.value = Array.isArray(data) ? data.map(normalizeCamera) : []
}

const fetchSettings = async () => {
  const res = await authFetch(endpoints.siteSettings)
  if (!res.ok) return
  const data = normalizeSiteSettings(await res.json())
  settings.site_name = data.site_name
  settings.announcement = data.announcement
  settings.logo_url = data.logo_url
  settings.footer_text = data.footer_text
}

const fetchMonitor = async () => {
  const [r1, r2] = await Promise.all([
    authFetch(endpoints.adminResources),
    authFetch(endpoints.adminNodes)
  ])
  if (r1.ok) resources.value = await r1.json()
  if (r2.ok) nodes.value = await r2.json()
}

const toggleRole = async (u: AdminUser) => {
  await authFetch(`${endpoints.adminUsers}/${u.id}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role_id: u.role_id === 0 ? 1 : 0 })
  })
  await fetchUsers()
}

const toggleStatus = async (u: AdminUser) => {
  await authFetch(`${endpoints.adminUsers}/${u.id}/status`, { method: 'PUT' })
  await fetchUsers()
}

const createCamera = async () => {
  if (!cameraForm.name.trim()) return
  await authFetch(endpoints.adminCameras, {
    method: 'POST',
    body: JSON.stringify({
      name: cameraForm.name,
      road_name: cameraForm.road_name || null,
      location: cameraForm.location || null,
      stream_url: cameraForm.stream_url || null,
      enabled: true
    })
  })
  cameraForm.name = ''
  cameraForm.road_name = ''
  cameraForm.location = ''
  cameraForm.stream_url = ''
  await fetchCameras()
}

const editCamera = async (camera: CameraItem) => {
  const name = window.prompt('名称', camera.name) ?? camera.name
  const roadName = window.prompt('路段', camera.road_name) ?? camera.road_name
  const location = window.prompt('位置', camera.location) ?? camera.location
  const streamUrl = window.prompt('stream_url', camera.stream_url) ?? camera.stream_url
  await authFetch(`${endpoints.adminCameras}/${camera.id}`, {
    method: 'PUT',
    body: JSON.stringify({
      name,
      road_name: roadName,
      location,
      stream_url: streamUrl,
      enabled: camera.enabled
    })
  })
  await fetchCameras()
}

const toggleCamera = async (camera: CameraItem) => {
  await authFetch(`${endpoints.adminCameras}/${camera.id}`, {
    method: 'PUT',
    body: JSON.stringify({
      name: camera.name,
      road_name: camera.road_name,
      location: camera.location,
      stream_url: camera.stream_url,
      enabled: !camera.enabled
    })
  })
  await fetchCameras()
}

const deleteCamera = async (id: number) => {
  if (!window.confirm('确定删除该摄像头？')) return
  await authFetch(`${endpoints.adminCameras}/${id}`, { method: 'DELETE' })
  await fetchCameras()
}

const saveSettings = async () => {
  await authFetch(endpoints.adminSiteSettings, {
    method: 'PUT',
    body: JSON.stringify({
      site_name: settings.site_name,
      logo_url: settings.logo_url,
      announcement: settings.announcement,
      footer_text: settings.footer_text
    })
  })
}

watch(tab, async (value) => {
  if (value === 'monitor') {
    await fetchMonitor()
    if (monitorTimer) window.clearInterval(monitorTimer)
    monitorTimer = window.setInterval(fetchMonitor, 5000)
  } else if (monitorTimer) {
    window.clearInterval(monitorTimer)
    monitorTimer = null
  }
})

onMounted(async () => {
  try {
    await ensureAdmin()
    await Promise.all([fetchUsers(), fetchCameras(), fetchSettings()])
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  if (monitorTimer) {
    window.clearInterval(monitorTimer)
  }
})
</script>

<style scoped>
.admin {
  display: grid;
  gap: 12px;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tabs button {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  height: 36px;
  padding: 0 12px;
  cursor: pointer;
}

.tabs button.active {
  background: #0ea5e9;
  color: #fff;
  border-color: #0ea5e9;
}

.card {
  border-radius: 12px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 8px 24px rgba(2, 6, 23, 0.08);
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}

th,
td {
  text-align: left;
  padding: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

input,
textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
}

button {
  border: 0;
  border-radius: 8px;
  background: #0284c7;
  color: #fff;
  padding: 8px 12px;
  cursor: pointer;
}

button.secondary {
  background: #475569;
  margin-right: 6px;
}

button.danger {
  background: #dc2626;
}

.settings-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 10px;
}

label {
  display: grid;
  gap: 6px;
}

.monitor-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

pre {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  overflow: auto;
  max-height: 360px;
}

.error {
  color: #dc2626;
  font-weight: 600;
}

@media (max-width: 1080px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .monitor-grid {
    grid-template-columns: 1fr;
  }
}
</style>
