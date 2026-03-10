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

          <!-- Tab 4: API 管理 -->
          <n-tab-pane name="apikeys" tab="API 管理">
            <div class="tab-toolbar">
              <n-button type="primary" @click="openApiModal(null)">
                <template #icon><n-icon><AddOutline /></n-icon></template>
                新建 API Key
              </n-button>
            </div>
            <n-data-table
              :columns="apiColumns"
              :data="apiClients"
              :loading="apiClientsLoading"
              :bordered="false"
              :single-line="false"
              size="small"
              :pagination="{ pageSize: 10 }"
              :row-key="(row: ApiClient) => row.id"
            />

            <!-- 创建/编辑弹窗 -->
            <n-modal
              v-model:show="apiModalVisible"
              preset="dialog"
              :title="apiEditing ? '编辑 API Key' : '新建 API Key'"
              positive-text="确定"
              negative-text="取消"
              :loading="apiSubmitting"
              @positive-click="handleApiSubmit"
              @negative-click="apiModalVisible = false"
              style="width: 560px"
            >
              <n-form
                ref="apiFormRef"
                :model="apiForm"
                :rules="apiFormRules"
                label-placement="left"
                label-width="100"
                class="modal-form"
              >
                <n-form-item label="名称" path="name">
                  <n-input v-model:value="apiForm.name" placeholder="请输入名称" />
                </n-form-item>
                <n-form-item label="描述" path="description">
                  <n-input v-model:value="apiForm.description" placeholder="可选描述" />
                </n-form-item>
                <n-form-item label="允许端点" path="allowed_endpoints">
                  <n-input
                    v-model:value="apiForm.allowed_endpoints_text"
                    type="textarea"
                    :rows="3"
                    placeholder="每行一个端点，留空表示允许全部，例如：&#10;/api/v1/roads_name&#10;/api/v1/frames_no_auth"
                  />
                </n-form-item>
                <n-form-item label="速率限制" path="rate_limit">
                  <n-input-number
                    v-model:value="apiForm.rate_limit"
                    :min="1"
                    :max="100000"
                    placeholder="每分钟请求上限"
                    style="width: 100%"
                  />
                </n-form-item>
              </n-form>
            </n-modal>

            <!-- 新 Key 显示弹窗 -->
            <n-modal
              v-model:show="newKeyModalVisible"
              preset="dialog"
              title="API Key 已生成"
              positive-text="已复制，关闭"
              @positive-click="newKeyModalVisible = false"
              style="width: 500px"
            >
              <n-alert type="warning" style="margin-bottom: 12px">
                请立即复制并妥善保存此 Key，关闭后将无法再次查看完整内容。
              </n-alert>
              <n-input-group>
                <n-input :value="newKeyValue" readonly style="font-family: monospace" />
                <n-button type="primary" @click="copyToClipboard(newKeyValue)">复制</n-button>
              </n-input-group>
            </n-modal>

            <!-- 用量统计弹窗 -->
            <n-modal
              v-model:show="usageModalVisible"
              preset="card"
              title="API 用量统计"
              style="width: 680px"
              :segmented="{ content: true }"
            >
              <n-spin :show="usageLoading">
                <template v-if="currentUsage">
                  <n-grid :x-gap="16" :y-gap="16" :cols="1" style="margin-bottom: 16px">
                    <n-gi>
                      <n-statistic label="近 30 天总调用次数" :value="currentUsage.total_calls" />
                    </n-gi>
                  </n-grid>

                  <n-card title="每日调用趋势" size="small" :bordered="false" style="margin-bottom: 16px">
                    <div v-if="currentUsage.daily_stats.length === 0" style="text-align:center;padding:24px">
                      <n-empty description="暂无调用记录" />
                    </div>
                    <div v-else class="usage-bar-chart">
                      <div
                        v-for="item in currentUsage.daily_stats"
                        :key="item.date"
                        class="usage-bar-item"
                      >
                        <div class="usage-bar-label">{{ formatDate(item.date) }}</div>
                        <div class="usage-bar-track">
                          <div
                            class="usage-bar-fill"
                            :style="{ width: getBarWidth(item.count, currentUsage!.daily_stats) + '%' }"
                          />
                        </div>
                        <div class="usage-bar-count">{{ item.count }}</div>
                      </div>
                    </div>
                  </n-card>

                  <n-card title="端点调用分布" size="small" :bordered="false">
                    <div v-if="currentUsage.endpoint_stats.length === 0" style="text-align:center;padding:24px">
                      <n-empty description="暂无端点统计" />
                    </div>
                    <n-data-table
                      v-else
                      :columns="usageEndpointColumns"
                      :data="currentUsage.endpoint_stats"
                      :bordered="false"
                      size="small"
                    />
                  </n-card>
                </template>
                <n-empty v-else-if="!usageLoading" description="暂无数据" />
              </n-spin>
            </n-modal>
          </n-tab-pane>

          <!-- Tab 5: 系统监控 -->
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
              <n-empty v-else description="暂无节点数据" />
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
  NSwitch,
  NIcon,
} from 'naive-ui'
import { AddOutline, EyeOutline, EyeOffOutline, CopyOutline, RefreshOutline, BarChartOutline, TrashOutline, CreateOutline } from '@vicons/ionicons5'
import { authFetch, endpoints } from '../lib/api'
import {
  normalizeAdminUser,
  normalizeCamera,
  normalizeSiteSettings,
  normalizeApiClient,
  normalizeApiClientUsage,
  type AdminUser,
  type CameraItem,
  type SiteSettings,
  type ApiClient,
  type ApiClientUsage,
} from '../lib/normalize'

const message = useMessage()

// --- State ---
const tab = ref<'users' | 'cameras' | 'settings' | 'apikeys' | 'monitor'>('users')
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

// API client state
const apiClients = ref<ApiClient[]>([])
const apiClientsLoading = ref(false)
const apiModalVisible = ref(false)
const apiEditing = ref<ApiClient | null>(null)
const apiSubmitting = ref(false)
const apiFormRef = ref<FormInst | null>(null)
const apiForm = reactive({
  name: '',
  description: '',
  allowed_endpoints_text: '',
  rate_limit: 1000,
})
const apiFormRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  rate_limit: [{ required: true, type: 'number', message: '请输入速率限制', trigger: 'change' }],
}

// Key reveal state per row
const revealedKeys = ref<Set<number>>(new Set())

// New key modal
const newKeyModalVisible = ref(false)
const newKeyValue = ref('')

// Usage modal
const usageModalVisible = ref(false)
const usageLoading = ref(false)
const currentUsage = ref<ApiClientUsage | null>(null)

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
    const arr = Array.isArray(data) ? data : (data?.content ?? [])
    users.value = arr.map(normalizeAdminUser)
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
    const arr = Array.isArray(data) ? data : (data?.content ?? [])
    cameras.value = arr.map(normalizeCamera)
  } finally {
    camerasLoading.value = false
  }
}

const fetchSettings = async () => {
  try {
    const res = await authFetch(endpoints.siteSettings)
    if (!res.ok) return
    const data = normalizeSiteSettings(await res.json())
    settings.site_name = data.site_name
    settings.announcement = data.announcement
    settings.logo_url = data.logo_url
    settings.footer_text = data.footer_text
  } catch {
    message.error('获取站点设置失败')
  }
}

const fetchMonitor = async () => {
  try {
    const [r1, r2] = await Promise.all([
      authFetch(endpoints.adminResources),
      authFetch(endpoints.adminNodes),
    ])
    if (r1.ok) resources.value = await r1.json()
    if (r2.ok) nodes.value = await r2.json()
  } catch {
    message.error('获取监控数据失败')
  }
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

// --- API client fetch ---
const fetchApiClients = async () => {
  apiClientsLoading.value = true
  try {
    const res = await authFetch(`${endpoints.adminApiClients}?page=0&size=100`)
    if (!res.ok) {
      message.error('获取 API Key 列表失败')
      return
    }
    const data = await res.json()
    const arr = Array.isArray(data) ? data : (data?.content ?? [])
    apiClients.value = arr.map(normalizeApiClient)
  } finally {
    apiClientsLoading.value = false
  }
}

// --- API client actions ---
const openApiModal = (client: ApiClient | null) => {
  apiEditing.value = client
  if (client) {
    apiForm.name = client.name
    apiForm.description = client.description
    apiForm.allowed_endpoints_text = client.allowed_endpoints.join('\n')
    apiForm.rate_limit = client.rate_limit
  } else {
    apiForm.name = ''
    apiForm.description = ''
    apiForm.allowed_endpoints_text = ''
    apiForm.rate_limit = 1000
  }
  apiModalVisible.value = true
}

const parseEndpoints = (text: string): string[] => {
  return text
    .split('\n')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
}

const handleApiSubmit = async () => {
  try {
    await apiFormRef.value?.validate()
  } catch {
    return false
  }
  apiSubmitting.value = true
  try {
    const body = {
      name: apiForm.name,
      description: apiForm.description || null,
      allowed_endpoints: parseEndpoints(apiForm.allowed_endpoints_text),
      rate_limit: apiForm.rate_limit,
    }
    if (apiEditing.value) {
      await authFetch(`${endpoints.adminApiClients}/${apiEditing.value.id}`, {
        method: 'PUT',
        body: JSON.stringify({ ...body, enabled: apiEditing.value.enabled }),
      })
      message.success('API Key 已更新')
      apiModalVisible.value = false
      await fetchApiClients()
    } else {
      const res = await authFetch(endpoints.adminApiClients, {
        method: 'POST',
        body: JSON.stringify(body),
      })
      if (!res.ok) {
        message.error('创建失败')
        return false
      }
      const created = normalizeApiClient(await res.json())
      apiModalVisible.value = false
      await fetchApiClients()
      newKeyValue.value = created.api_key
      newKeyModalVisible.value = true
    }
  } catch {
    message.error('操作失败')
  } finally {
    apiSubmitting.value = false
  }
  return false
}

const toggleApiEnabled = async (client: ApiClient) => {
  try {
    await authFetch(`${endpoints.adminApiClients}/${client.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: client.name,
        description: client.description,
        allowed_endpoints: client.allowed_endpoints,
        rate_limit: client.rate_limit,
        enabled: !client.enabled,
      }),
    })
    await fetchApiClients()
    message.success(client.enabled ? '已禁用' : '已启用')
  } catch {
    message.error('切换状态失败')
  }
}

const regenerateKey = async (client: ApiClient) => {
  try {
    const res = await authFetch(`${endpoints.adminApiClients}/${client.id}/regenerate`, {
      method: 'POST',
    })
    if (!res.ok) {
      message.error('重新生成失败')
      return
    }
    const updated = normalizeApiClient(await res.json())
    await fetchApiClients()
    newKeyValue.value = updated.api_key
    newKeyModalVisible.value = true
  } catch {
    message.error('重新生成失败')
  }
}

const deleteApiClient = async (id: number) => {
  try {
    await authFetch(`${endpoints.adminApiClients}/${id}`, { method: 'DELETE' })
    await fetchApiClients()
    message.success('已删除')
  } catch {
    message.error('删除失败')
  }
}

const openUsageModal = async (client: ApiClient) => {
  currentUsage.value = null
  usageModalVisible.value = true
  usageLoading.value = true
  try {
    const res = await authFetch(`${endpoints.adminApiClients}/${client.id}/usage?days=30`)
    if (!res.ok) {
      message.error('获取用量失败')
      return
    }
    currentUsage.value = normalizeApiClientUsage(await res.json())
  } catch {
    message.error('获取用量失败')
  } finally {
    usageLoading.value = false
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

const maskKey = (key: string): string => {
  if (!key || key.length <= 8) return key
  return `${key.slice(0, 4)}${'*'.repeat(Math.max(key.length - 8, 4))}${key.slice(-4)}`
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const formatDateTime = (dateStr: string | null): string => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const getBarWidth = (count: number, stats: { date: string; count: number }[]): number => {
  const max = Math.max(...stats.map((s) => s.count), 1)
  return Math.round((count / max) * 100)
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

// --- API client table columns ---
const apiColumns: DataTableColumns<ApiClient> = [
  { title: '名称', key: 'name', ellipsis: { tooltip: true }, width: 140 },
  {
    title: 'API Key',
    key: 'api_key',
    width: 220,
    render(row) {
      const revealed = revealedKeys.value.has(row.id)
      const displayKey = revealed ? row.api_key : maskKey(row.api_key)
      return h('div', { style: 'display:flex;align-items:center;gap:6px' }, [
        h('span', { style: 'font-family:monospace;font-size:12px;flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap' }, displayKey),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          onClick: () => {
            const next = new Set(revealedKeys.value)
            if (next.has(row.id)) next.delete(row.id)
            else next.add(row.id)
            revealedKeys.value = next
          },
        }, {
          icon: () => h(NIcon, {}, { default: () => h(revealed ? EyeOffOutline : EyeOutline) }),
        }),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          onClick: () => copyToClipboard(row.api_key),
        }, {
          icon: () => h(NIcon, {}, { default: () => h(CopyOutline) }),
        }),
      ])
    },
  },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  {
    title: '速率限制',
    key: 'rate_limit',
    width: 100,
    render(row) {
      return h('span', {}, `${row.rate_limit}/min`)
    },
  },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render(row) {
      return h(NSwitch, {
        value: row.enabled,
        size: 'small',
        onUpdateValue: () => toggleApiEnabled(row),
      })
    },
  },
  {
    title: '创建时间',
    key: 'created_at',
    width: 150,
    render(row) {
      return h('span', {}, formatDateTime(row.created_at))
    },
  },
  {
    title: '最近使用',
    key: 'last_used_at',
    width: 150,
    render(row) {
      return h('span', {}, formatDateTime(row.last_used_at))
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render(row) {
      return h('div', { style: 'display:flex;gap:6px;flex-wrap:wrap' }, [
        h(NButton, {
          size: 'small',
          secondary: true,
          onClick: () => openApiModal(row),
        }, {
          icon: () => h(NIcon, {}, { default: () => h(CreateOutline) }),
          default: () => '编辑',
        }),
        h(NPopconfirm, {
          onPositiveClick: () => regenerateKey(row),
        }, {
          trigger: () => h(NButton, {
            size: 'small',
            secondary: true,
            type: 'warning',
          }, {
            icon: () => h(NIcon, {}, { default: () => h(RefreshOutline) }),
            default: () => '重新生成',
          }),
          default: () => '重新生成后旧 Key 立即失效，确认？',
        }),
        h(NButton, {
          size: 'small',
          secondary: true,
          type: 'info',
          onClick: () => openUsageModal(row),
        }, {
          icon: () => h(NIcon, {}, { default: () => h(BarChartOutline) }),
          default: () => '用量',
        }),
        h(NPopconfirm, {
          onPositiveClick: () => deleteApiClient(row.id),
        }, {
          trigger: () => h(NButton, {
            size: 'small',
            type: 'error',
            secondary: true,
          }, {
            icon: () => h(NIcon, {}, { default: () => h(TrashOutline) }),
          }),
          default: () => '确定删除此 API Key？',
        }),
      ])
    },
  },
]

// --- Usage endpoint columns ---
const usageEndpointColumns: DataTableColumns = [
  { title: '端点', key: 'endpoint', ellipsis: { tooltip: true } },
  { title: '调用次数', key: 'count', width: 100 },
]

// --- Monitor computed ---
const resourceMetrics = computed(() => {
  const r = resources.value as Record<string, unknown>
  const mem = (r.memory as Record<string, unknown>) ?? {}
  const disk = (r.disk as Record<string, unknown>) ?? {}
  return {
    cpu:    Math.round(Number(r.cpu_percent ?? r.cpuPercent ?? 0)),
    memory: Math.round(Number(mem.percent ?? 0)),
    disk:   Math.round(Number(disk.percent ?? 0)),
  }
})

const getProgressColor = (pct: number) => {
  if (pct >= 90) return '#d03050'
  if (pct >= 70) return '#f0a020'
  return '#18a058'
}

// Node list: parse nodes map from { nodes: { name: {...} } } response
const nodeList = computed(() => {
  const n = nodes.value as Record<string, unknown>
  const nodeMap = (n.nodes ?? n) as Record<string, unknown>
  if (typeof nodeMap !== 'object' || nodeMap === null || Array.isArray(nodeMap)) return []
  return Object.entries(nodeMap).map(([nodeName, info]) => ({
    name: nodeName,
    ...(typeof info === 'object' && info !== null ? (info as Record<string, unknown>) : {}),
  }))
})

const nodeColumns: DataTableColumns = [
  { title: '节点名称', key: 'name', ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'online',
    width: 90,
    render(row: Record<string, unknown>) {
      const isOnline = Boolean(row.online)
      return h(NTag, {
        type: isOnline ? 'success' : 'error',
        size: 'small',
      }, { default: () => (isOnline ? '在线' : '离线') })
    },
  },
  {
    title: '延迟',
    key: 'latency_ms',
    width: 90,
    render(row: Record<string, unknown>) {
      const ms = row.latency_ms
      return h('span', {}, ms != null ? `${ms} ms` : '—')
    },
  },
  {
    title: '错误次数',
    key: 'error_count',
    width: 90,
    render(row: Record<string, unknown>) {
      return h('span', {}, row.error_count != null ? String(row.error_count) : '—')
    },
  },
  {
    title: '最近成功时间',
    key: 'last_success_time',
    ellipsis: { tooltip: true },
    render(row: Record<string, unknown>) {
      const t = row.last_success_time
      return h('span', {}, t ? formatDateTime(String(t)) : '—')
    },
  },
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
  } else {
    if (monitorTimer) {
      window.clearInterval(monitorTimer)
      monitorTimer = null
    }
    if (value === 'apikeys' && apiClients.value.length === 0) {
      await fetchApiClients()
    }
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

.usage-bar-chart {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 4px 0;
}

.usage-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.usage-bar-label {
  width: 36px;
  flex-shrink: 0;
  color: #6b7280;
  text-align: right;
}

.usage-bar-track {
  flex: 1;
  background: #f0f0f0;
  border-radius: 4px;
  height: 14px;
  overflow: hidden;
}

.usage-bar-fill {
  height: 100%;
  background: #2080f0;
  border-radius: 4px;
  transition: width 0.3s ease;
  min-width: 2px;
}

.usage-bar-count {
  width: 40px;
  flex-shrink: 0;
  color: #374151;
  font-weight: 500;
  text-align: right;
}
</style>
