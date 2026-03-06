<template>
  <section class="admin-page">
    <n-spin :show="loading" description="加载中...">
      <n-result v-if="errorText" status="error" :title="errorText" description="请确认您拥有管理员权限">
        <template #footer>
          <n-button @click="retryInit">重试</n-button>
        </template>
      </n-result>

      <template v-if="!loading && !errorText">
        <n-tabs v-model:value="tab" type="line" animated>
          <!-- Tab 1: 用户管理 -->
          <n-tab-pane name="users" tab="用户管理">
            <n-data-table
              :columns="userColumns"
              :data="users"
              :loading="usersLoading"
              :bordered="false"
              :single-line="false"
              size="small"
              :pagination="{ pageSize: 10 }"
              :row-key="(row: AdminUser) => row.id"
            />
          </n-tab-pane>

          <!-- Tab 2: 摄像头管理 -->
          <n-tab-pane name="cameras" tab="摄像头管理">
            <div class="tab-toolbar">
              <n-button type="primary" @click="openCameraModal(null)">
                <template #icon><n-icon><AddOutline /></n-icon></template>
                新增摄像头
              </n-button>
            </div>
            <n-data-table
              :columns="cameraColumns"
              :data="cameras"
              :loading="camerasLoading"
              :bordered="false"
              :single-line="false"
              size="small"
              :pagination="{ pageSize: 10 }"
              :row-key="(row: CameraItem) => row.id"
            />
            <!-- 摄像头编辑弹窗 -->
            <n-modal
              v-model:show="cameraModalVisible"
              preset="dialog"
              :title="cameraEditing ? '编辑摄像头' : '新增摄像头'"
              positive-text="确定"
              negative-text="取消"
              :loading="cameraSubmitting"
              @positive-click="handleCameraSubmit"
              @negative-click="cameraModalVisible = false"
              style="width: 520px"
            >
              <n-form
                ref="cameraFormRef"
                :model="cameraForm"
                :rules="cameraRules"
                label-placement="left"
                label-width="80"
                class="modal-form"
              >
                <n-form-item label="名称" path="name">
                  <n-input v-model:value="cameraForm.name" placeholder="请输入摄像头名称" />
                </n-form-item>
                <n-form-item label="RTSP URL" path="stream_url">
                  <n-input v-model:value="cameraForm.stream_url" placeholder="请输入流地址" />
                </n-form-item>
                <n-form-item label="路段" path="road_name">
                  <n-input v-model:value="cameraForm.road_name" placeholder="请输入道路名称" />
                </n-form-item>
                <n-form-item label="位置" path="location">
                  <n-input v-model:value="cameraForm.location" placeholder="请输入位置信息" />
                </n-form-item>
                <n-form-item label="启用">
                  <n-switch v-model:value="cameraForm.enabled" />
                </n-form-item>
              </n-form>
            </n-modal>
          </n-tab-pane>

          <!-- Tab 3: 站点设置 -->
          <n-tab-pane name="settings" tab="站点设置">
            <n-card :bordered="false">
              <n-form
                label-placement="left"
                label-width="100"
                :model="settings"
              >
                <n-form-item label="站点名称">
                  <n-input v-model:value="settings.site_name" placeholder="请输入站点名称" />
                </n-form-item>
                <n-form-item label="Logo URL">
                  <n-input v-model:value="settings.logo_url" placeholder="请输入 Logo 地址" />
                </n-form-item>
                <n-form-item label="公告内容">
                  <n-input
                    v-model:value="settings.announcement"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入公告内容"
                  />
                </n-form-item>
                <n-form-item label="页脚文字">
                  <n-input v-model:value="settings.footer_text" placeholder="请输入页脚文字" />
                </n-form-item>
                <n-form-item label=" ">
                  <n-button type="primary" :loading="settingsSaving" @click="saveSettings">
                    保存设置
                  </n-button>
                </n-form-item>
              </n-form>
            </n-card>
          </n-tab-pane>

          <!-- Tab 4: 系统监控 -->
          <n-tab-pane name="monitor" tab="系统监控">
            <n-grid :x-gap="16" :y-gap="16" :cols="3" responsive="screen" item-responsive>
              <n-gi span="3 m:1">
                <n-card title="CPU 使用率" size="small" :bordered="false" class="monitor-card">
                  <n-progress
                    type="circle"
                    :percentage="resourceMetrics.cpu"
                    :color="getProgressColor(resourceMetrics.cpu)"
                  />
                  <n-text class="metric-label">{{ resourceMetrics.cpu }}%</n-text>
                </n-card>
              </n-gi>
              <n-gi span="3 m:1">
                <n-card title="内存使用率" size="small" :bordered="false" class="monitor-card">
                  <n-progress
                    type="circle"
                    :percentage="resourceMetrics.memory"
                    :color="getProgressColor(resourceMetrics.memory)"
                  />
                  <n-text class="metric-label">{{ resourceMetrics.memory }}%</n-text>
                </n-card>
              </n-gi>
              <n-gi span="3 m:1">
                <n-card title="磁盘使用率" size="small" :bordered="false" class="monitor-card">
                  <n-progress
                    type="circle"
                    :percentage="resourceMetrics.disk"
                    :color="getProgressColor(resourceMetrics.disk)"
                  />
                  <n-text class="metric-label">{{ resourceMetrics.disk }}%</n-text>
                </n-card>
              </n-gi>
            </n-grid>

            <n-card title="节点状态" size="small" :bordered="false" style="margin-top: 16px">
              <n-data-table
                v-if="nodeList.length"
                :columns="nodeColumns"
                :data="nodeList"
                :bordered="false"
                :single-line="false"
                size="small"
              />
              <n-descriptions v-else-if="Object.keys(nodes).length" bordered :column="2">
                <n-descriptions-item
                  v-for="(val, key) in nodes"
                  :key="String(key)"
                  :label="String(key)"
                >
                  {{ typeof val === 'object' ? JSON.stringify(val) : val }}
                </n-descriptions-item>
              </n-descriptions>
              <n-empty v-else description="暂无节点数据" />
            </n-card>

            <n-card
              v-if="Object.keys(resources).length"
              title="资源详情"
              size="small"
              :bordered="false"
              style="margin-top: 16px"
            >
              <n-descriptions bordered :column="2">
                <n-descriptions-item
                  v-for="(val, key) in resources"
                  :key="String(key)"
                  :label="String(key)"
                >
                  {{ typeof val === 'object' ? JSON.stringify(val) : val }}
                </n-descriptions-item>
              </n-descriptions>
            </n-card>
          </n-tab-pane>
        </n-tabs>
      </template>
    </n-spin>
  </section>
</template>

<script setup lang="ts">
import { h, onMounted, onUnmounted, reactive, ref, computed, watch } from 'vue'
import {
  useMessage,
  type FormInst,
  type FormRules,
  type DataTableColumns,
  NTag,
  NButton,
  NPopconfirm,
} from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import { authFetch, endpoints } from '../lib/api'
import {
  normalizeAdminUser,
  normalizeCamera,
  normalizeSiteSettings,
  type AdminUser,
  type CameraItem,
  type SiteSettings,
} from '../lib/normalize'

const message = useMessage()

// --- State ---
const tab = ref<'users' | 'cameras' | 'settings' | 'monitor'>('users')
const loading = ref(true)
const errorText = ref('')

const users = ref<AdminUser[]>([])
const usersLoading = ref(false)
const cameras = ref<CameraItem[]>([])
const camerasLoading = ref(false)

const settings = reactive<SiteSettings>({
  site_name: '',
  announcement: '',
  logo_url: '',
  footer_text: '',
})
const settingsSaving = ref(false)

const resources = ref<Record<string, unknown>>({})
const nodes = ref<Record<string, unknown>>({})
let monitorTimer: number | null = null

// Camera modal
const cameraModalVisible = ref(false)
const cameraEditing = ref<CameraItem | null>(null)
const cameraSubmitting = ref(false)
const cameraFormRef = ref<FormInst | null>(null)
const cameraForm = reactive({
  name: '',
  road_name: '',
  location: '',
  stream_url: '',
  enabled: true,
})
const cameraRules: FormRules = {
  name: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
}

// --- API calls ---
const ensureAdmin = async () => {
  const me = await authFetch(endpoints.me)
  if (!me.ok) throw new Error('无法验证登录状态')
  const data = await me.json()
  const role = Number(data?.role_id ?? data?.roleId ?? 1)
  if (role !== 0) throw new Error('仅管理员可访问')
}

const fetchUsers = async () => {
  usersLoading.value = true
  try {
    const res = await authFetch(endpoints.adminUsers)
    if (!res.ok) {
      message.error('获取用户列表失败')
      return
    }
    const data = await res.json()
    users.value = Array.isArray(data) ? data.map(normalizeAdminUser) : []
  } finally {
    usersLoading.value = false
  }
}

const fetchCameras = async () => {
  camerasLoading.value = true
  try {
    const res = await authFetch(endpoints.adminCameras)
    if (!res.ok) {
      message.error('获取摄像头列表失败')
      return
    }
    const data = await res.json()
    cameras.value = Array.isArray(data) ? data.map(normalizeCamera) : []
  } finally {
    camerasLoading.value = false
  }
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
    authFetch(endpoints.adminNodes),
  ])
  if (r1.ok) resources.value = await r1.json()
  if (r2.ok) nodes.value = await r2.json()
}

// --- User actions ---
const changeRole = async (u: AdminUser, newRole: number) => {
  try {
    await authFetch(`${endpoints.adminUsers}/${u.id}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role_id: newRole }),
    })
    await fetchUsers()
    message.success('角色已更新')
  } catch {
    message.error('更新角色失败，请重试')
  }
}

const toggleStatus = async (u: AdminUser) => {
  try {
    await authFetch(`${endpoints.adminUsers}/${u.id}/status`, { method: 'PUT' })
    await fetchUsers()
    message.success('状态已更新')
  } catch {
    message.error('更新状态失败，请重试')
  }
}

// --- Camera actions ---
const openCameraModal = (camera: CameraItem | null) => {
  cameraEditing.value = camera
  if (camera) {
    cameraForm.name = camera.name
    cameraForm.road_name = camera.road_name
    cameraForm.location = camera.location
    cameraForm.stream_url = camera.stream_url
    cameraForm.enabled = camera.enabled
  } else {
    cameraForm.name = ''
    cameraForm.road_name = ''
    cameraForm.location = ''
    cameraForm.stream_url = ''
    cameraForm.enabled = true
  }
  cameraModalVisible.value = true
}

const handleCameraSubmit = async () => {
  try {
    await cameraFormRef.value?.validate()
  } catch {
    return false
  }
  cameraSubmitting.value = true
  try {
    const body = {
      name: cameraForm.name,
      road_name: cameraForm.road_name || null,
      location: cameraForm.location || null,
      stream_url: cameraForm.stream_url || null,
      enabled: cameraForm.enabled,
    }
    if (cameraEditing.value) {
      await authFetch(`${endpoints.adminCameras}/${cameraEditing.value.id}`, {
        method: 'PUT',
        body: JSON.stringify(body),
      })
      message.success('摄像头已更新')
    } else {
      await authFetch(endpoints.adminCameras, {
        method: 'POST',
        body: JSON.stringify(body),
      })
      message.success('摄像头已添加')
    }
    cameraModalVisible.value = false
    await fetchCameras()
  } catch {
    message.error('操作失败')
  } finally {
    cameraSubmitting.value = false
  }
  return false
}

const toggleCamera = async (camera: CameraItem) => {
  try {
    await authFetch(`${endpoints.adminCameras}/${camera.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: camera.name,
        road_name: camera.road_name,
        location: camera.location,
        stream_url: camera.stream_url,
        enabled: !camera.enabled,
      }),
    })
    await fetchCameras()
    message.success(camera.enabled ? '已禁用' : '已启用')
  } catch {
    message.error('切换摄像头状态失败，请重试')
  }
}

const deleteCamera = async (id: number) => {
  try {
    await authFetch(`${endpoints.adminCameras}/${id}`, { method: 'DELETE' })
    await fetchCameras()
    message.success('摄像头已删除')
  } catch {
    message.error('删除摄像头失败，请重试')
  }
}

const saveSettings = async () => {
  settingsSaving.value = true
  try {
    await authFetch(endpoints.adminSiteSettings, {
      method: 'PUT',
      body: JSON.stringify({
        site_name: settings.site_name,
        logo_url: settings.logo_url,
        announcement: settings.announcement,
        footer_text: settings.footer_text,
      }),
    })
    message.success('设置已保存')
  } catch {
    message.error('保存失败')
  } finally {
    settingsSaving.value = false
  }
}

// --- User table columns ---
const userColumns: DataTableColumns<AdminUser> = [
  { title: '用户名', key: 'username', ellipsis: { tooltip: true } },
  { title: '邮箱', key: 'email', ellipsis: { tooltip: true } },
  {
    title: '角色',
    key: 'role_id',
    width: 120,
    render(row) {
      return h(NTag, {
        type: row.role_id === 0 ? 'warning' : 'info',
        size: 'small',
        round: true,
      }, { default: () => (row.role_id === 0 ? '管理员' : '用户') })
    },
  },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render(row) {
      return h(NTag, {
        type: row.enabled ? 'success' : 'error',
        size: 'small',
      }, { default: () => (row.enabled ? '启用' : '禁用') })
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          size: 'small',
          secondary: true,
          onClick: () => changeRole(row, row.role_id === 0 ? 1 : 0),
        }, { default: () => '切换角色' }),
        h(NButton, {
          size: 'small',
          secondary: true,
          type: row.enabled ? 'error' : 'success',
          onClick: () => toggleStatus(row),
        }, { default: () => (row.enabled ? '禁用' : '启用') }),
      ])
    },
  },
]

// --- Camera table columns ---
const cameraColumns: DataTableColumns<CameraItem> = [
  { title: '名称', key: 'name', ellipsis: { tooltip: true } },
  { title: '路段', key: 'road_name', ellipsis: { tooltip: true } },
  { title: '流地址', key: 'stream_url', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render(row) {
      return h(NTag, {
        type: row.enabled ? 'success' : 'error',
        size: 'small',
      }, { default: () => (row.enabled ? '启用' : '禁用') })
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          size: 'small',
          secondary: true,
          onClick: () => openCameraModal(row),
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          secondary: true,
          type: row.enabled ? 'error' : 'success',
          onClick: () => toggleCamera(row),
        }, { default: () => (row.enabled ? '禁用' : '启用') }),
        h(NPopconfirm, {
          onPositiveClick: () => deleteCamera(row.id),
        }, {
          trigger: () => h(NButton, {
            size: 'small',
            type: 'error',
            secondary: true,
          }, { default: () => '删除' }),
          default: () => '确定删除该摄像头？',
        }),
      ])
    },
  },
]

// --- Monitor computed ---
const resourceMetrics = computed(() => {
  const r = resources.value as Record<string, unknown>
  return {
    cpu: Math.round(Number(r.cpu_percent ?? r.cpuPercent ?? 0)),
    memory: Math.round(Number(r.memory_percent ?? r.memoryPercent ?? 0)),
    disk: Math.round(Number(r.disk_percent ?? r.diskPercent ?? 0)),
  }
})

const getProgressColor = (pct: number) => {
  if (pct >= 90) return '#d03050'
  if (pct >= 70) return '#f0a020'
  return '#18a058'
}

// Node list: try to parse nodes as array for table display
const nodeList = computed(() => {
  const n = nodes.value
  if (Array.isArray(n)) return n
  return []
})

const nodeColumns: DataTableColumns = [
  { title: '节点', key: 'name', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row: Record<string, unknown>) {
      const status = String(row.status ?? row.state ?? 'unknown')
      const isOnline = ['online', 'running', 'healthy', 'up'].includes(status.toLowerCase())
      return h(NTag, {
        type: isOnline ? 'success' : 'error',
        size: 'small',
      }, { default: () => status })
    },
  },
  { title: '地址', key: 'address', ellipsis: { tooltip: true } },
]

// --- Init & lifecycle ---
const retryInit = async () => {
  loading.value = true
  errorText.value = ''
  try {
    await ensureAdmin()
    await Promise.all([fetchUsers(), fetchCameras(), fetchSettings()])
  } catch (err) {
    errorText.value = err instanceof Error ? err.message : '加载失败'
  } finally {
    loading.value = false
  }
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
.admin-page {
  max-width: 1200px;
  margin: 0 auto;
}

.tab-toolbar {
  margin-bottom: 12px;
}

.modal-form {
  margin-top: 16px;
}

.monitor-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.monitor-card :deep(.n-progress) {
  width: 120px;
}

.metric-label {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
}
</style>
