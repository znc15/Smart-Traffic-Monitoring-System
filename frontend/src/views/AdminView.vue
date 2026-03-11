<template>
  <section class="admin-page">
    <NSpin :show="loading" description="加载中...">
      <NResult
        v-if="errorText"
        status="error"
        :title="errorText"
        description="请确认当前账号拥有管理员权限"
      >
        <template #footer>
          <NButton @click="retryInit">重试</NButton>
        </template>
      </NResult>

      <template v-else>
        <NTabs v-model:value="tab" type="line" animated>
          <NTabPane name="users" tab="用户管理">
            <NDataTable
              :columns="userColumns"
              :data="users"
              :bordered="false"
              :single-line="false"
              size="small"
              :pagination="{ pageSize: 10 }"
            />
          </NTabPane>

          <NTabPane name="cameras" tab="摄像头管理">
            <div class="toolbar">
              <NButton type="primary" @click="openCameraModal(null)">新增摄像头</NButton>
            </div>
            <NDataTable
              :columns="cameraColumns"
              :data="cameras"
              :loading="camerasLoading"
              :bordered="false"
              :single-line="false"
              size="small"
              :pagination="{ pageSize: 10 }"
            />
          </NTabPane>

          <NTabPane name="settings" tab="站点设置">
            <NCard :bordered="false">
              <NForm :model="settings" label-placement="left" label-width="100">
                <NFormItem label="站点名称">
                  <NInput v-model:value="settings.site_name" />
                </NFormItem>
                <NFormItem label="公告内容">
                  <NInput v-model:value="settings.announcement" type="textarea" :rows="4" />
                </NFormItem>
                <NFormItem label="Logo URL">
                  <NInput v-model:value="settings.logo_url" />
                </NFormItem>
                <NFormItem label="页脚文案">
                  <NInput v-model:value="settings.footer_text" />
                </NFormItem>
                <NFormItem label="高德地图 Key">
                  <NInput v-model:value="settings.amap_key" clearable />
                  <div class="form-hint">
                    留空表示继续使用部署阶段的 `VITE_AMAP_KEY`，保存后刷新地图页即可采用后台值。
                  </div>
                </NFormItem>
                <NFormItem label=" ">
                  <NButton type="primary" :loading="settingsSaving" @click="saveSettings">保存设置</NButton>
                </NFormItem>
              </NForm>
            </NCard>
          </NTabPane>

          <NTabPane name="monitor" tab="系统监控">
            <NGrid :x-gap="16" :y-gap="16" :cols="3" responsive="screen" item-responsive>
              <NGi span="3 m:1">
                <NCard size="small" title="CPU" :bordered="false">
                  <NStatistic :value="resourceMetrics.cpu" suffix="%" />
                </NCard>
              </NGi>
              <NGi span="3 m:1">
                <NCard size="small" title="内存" :bordered="false">
                  <NStatistic :value="resourceMetrics.memory" suffix="%" />
                </NCard>
              </NGi>
              <NGi span="3 m:1">
                <NCard size="small" title="磁盘" :bordered="false">
                  <NStatistic :value="resourceMetrics.disk" suffix="%" />
                </NCard>
              </NGi>
            </NGrid>

            <NCard title="节点状态" size="small" :bordered="false" style="margin-top: 16px">
              <div class="node-toolbar">
                <div class="node-toolbar__filters">
                  <NButton
                    size="small"
                    :type="nodeHealthFilter === 'all' ? 'primary' : 'default'"
                    @click="nodeHealthFilter = 'all'"
                  >
                    全部 {{ nodeHealthCounts.all }}
                  </NButton>
                  <NButton
                    size="small"
                    :type="nodeHealthFilter === 'issues' ? 'primary' : 'default'"
                    @click="nodeHealthFilter = 'issues'"
                  >
                    异常 {{ nodeHealthCounts.issues }}
                  </NButton>
                  <NButton
                    size="small"
                    :type="nodeHealthFilter === 'degraded' ? 'warning' : 'default'"
                    @click="nodeHealthFilter = 'degraded'"
                  >
                    降级 {{ nodeHealthCounts.degraded }}
                  </NButton>
                  <NButton
                    size="small"
                    :type="nodeHealthFilter === 'offline' ? 'error' : 'default'"
                    @click="nodeHealthFilter = 'offline'"
                  >
                    离线 {{ nodeHealthCounts.offline }}
                  </NButton>
                  <NButton
                    size="small"
                    :type="nodeHealthFilter === 'online' ? 'success' : 'default'"
                    @click="nodeHealthFilter = 'online'"
                  >
                    在线 {{ nodeHealthCounts.online }}
                  </NButton>
                </div>
                <NSelect
                  v-model:value="nodeSortKey"
                  size="small"
                  :options="nodeSortOptions"
                  style="width: 200px"
                />
              </div>
              <NDataTable
                :columns="nodeColumns"
                :data="displayedNodeList"
                :bordered="false"
                :single-line="false"
                size="small"
                :pagination="{ pageSize: 8 }"
              />
            </NCard>

            <NCard title="事件日志" size="small" :bordered="false" style="margin-top: 16px">
              <NDataTable
                :columns="eventColumns"
                :data="eventLogs"
                :loading="eventsLoading"
                :bordered="false"
                :single-line="false"
                size="small"
                :pagination="{ pageSize: 8 }"
              />
            </NCard>
          </NTabPane>
        </NTabs>
      </template>
    </NSpin>

    <NModal
      v-model:show="cameraModalVisible"
      preset="dialog"
      :title="cameraEditing ? '编辑摄像头' : '新增摄像头'"
      positive-text="保存"
      negative-text="取消"
      :loading="cameraSubmitting"
      style="width: 640px"
      @positive-click="handleCameraSubmit"
      @negative-click="cameraModalVisible = false"
    >
      <NForm ref="cameraFormRef" :model="cameraForm" :rules="cameraRules" label-placement="left" label-width="92">
        <NFormItem label="名称" path="name">
          <NInput v-model:value="cameraForm.name" />
        </NFormItem>
        <NFormItem label="路段" path="road_name">
          <NInput v-model:value="cameraForm.road_name" />
        </NFormItem>
        <NFormItem label="节点地址" path="node_url">
          <NInput v-model:value="cameraForm.node_url" placeholder="http://192.168.1.100:8000" />
        </NFormItem>
        <NFormItem label="流地址" path="stream_url">
          <NInput v-model:value="cameraForm.stream_url" />
        </NFormItem>
        <NFormItem label="节点 ID" path="edge_node_id">
          <NInput v-model:value="cameraForm.edge_node_id" />
        </NFormItem>
        <NFormItem label="节点密钥" path="node_api_key">
          <NInput v-model:value="cameraForm.node_api_key" type="password" show-password-on="click" />
        </NFormItem>
        <NFormItem label="位置" path="location">
          <NInput v-model:value="cameraForm.location" />
        </NFormItem>
        <NFormItem label="纬度" path="latitude">
          <NInputNumber v-model:value="cameraForm.latitude" :step="0.000001" style="width: 100%" />
        </NFormItem>
        <NFormItem label="经度" path="longitude">
          <NInputNumber v-model:value="cameraForm.longitude" :step="0.000001" style="width: 100%" />
        </NFormItem>
        <NFormItem label="启用">
          <NSwitch v-model:value="cameraForm.enabled" />
        </NFormItem>
      </NForm>
    </NModal>

    <NModal
      v-model:show="nodeConfigModalVisible"
      preset="dialog"
      :title="`节点配置 - ${currentNodeLabel}`"
      positive-text="下发配置"
      negative-text="取消"
      :loading="nodeConfigSubmitting"
      style="width: 720px"
      @positive-click="saveNodeConfig"
      @negative-click="nodeConfigModalVisible = false"
    >
      <NAlert type="info" style="margin-bottom: 12px">
        直接编辑 JSON 配置，支持 ROI、测速、事件阈值与上报间隔。
      </NAlert>
      <NInput v-model:value="nodeConfigText" type="textarea" :rows="18" />
    </NModal>
  </section>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NResult,
  NSelect,
  NSpin,
  NStatistic,
  NSwitch,
  NTabPane,
  NTabs,
  NTag,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  useMessage,
} from 'naive-ui'
import { authFetch, endpoints, ensureOk } from '../lib/api'
import {
  normalizeAdminUser,
  normalizeAdminNodeHealth,
  normalizeCamera,
  normalizeSiteSettings,
  normalizeTrafficEvent,
  type AdminNodeHealth,
  type AdminUser,
  type CameraItem,
  type SiteSettings,
  type TrafficEventItem,
} from '../lib/normalize'

const message = useMessage()

const tab = ref<'users' | 'cameras' | 'settings' | 'monitor'>('users')
const loading = ref(true)
const errorText = ref('')
const users = ref<AdminUser[]>([])
const cameras = ref<CameraItem[]>([])
const camerasLoading = ref(false)
const settingsSaving = ref(false)
const resources = ref<Record<string, unknown>>({})
const nodes = ref<Record<string, unknown>>({})
const eventLogs = ref<TrafficEventItem[]>([])
const eventsLoading = ref(false)
let monitorTimer: number | null = null
const nodeHealthFilter = ref<'all' | 'issues' | 'degraded' | 'offline' | 'online'>('all')
const nodeSortKey = ref<'severity' | 'last_success_desc' | 'latency_desc' | 'name'>('severity')

const settings = reactive<SiteSettings>({
  site_name: '',
  announcement: '',
  logo_url: '',
  footer_text: '',
  amap_key: '',
})

const cameraModalVisible = ref(false)
const cameraEditing = ref<CameraItem | null>(null)
const cameraSubmitting = ref(false)
const cameraFormRef = ref<FormInst | null>(null)
const cameraForm = reactive({
  name: '',
  road_name: '',
  location: '',
  stream_url: '',
  node_url: '',
  edge_node_id: '',
  node_api_key: '',
  latitude: null as number | null,
  longitude: null as number | null,
  enabled: true,
})
const cameraRules: FormRules = {
  name: [{ required: true, message: '请输入摄像头名称', trigger: 'blur' }],
}

const nodeConfigModalVisible = ref(false)
const nodeConfigSubmitting = ref(false)
const currentNodeLabel = ref('')
const currentNodeCameraId = ref<number | null>(null)
const nodeConfigText = ref('')

const ensureAdmin = async () => {
  const res = await authFetch(endpoints.me)
  if (!res.ok) throw new Error('无法验证登录状态')
  const data = await res.json()
  const role = Number(data?.role_id ?? data?.roleId ?? 1)
  if (role !== 0) throw new Error('仅管理员可访问')
}

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—'
  return new Date(value).toLocaleString('zh-CN')
}

const nodeSeverityRank: Record<AdminNodeHealth['health_status'], number> = {
  offline: 0,
  degraded: 1,
  online: 2,
}

const healthLabelMap: Record<AdminNodeHealth['health_status'], string> = {
  online: '在线',
  degraded: '降级',
  offline: '离线',
}

const nodeSortOptions = [
  { label: '按严重度', value: 'severity' },
  { label: '按最近成功时间', value: 'last_success_desc' },
  { label: '按延迟高到低', value: 'latency_desc' },
  { label: '按节点名称', value: 'name' },
]

const toTimestamp = (value: string | null | undefined) => {
  if (!value) return 0
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const summarizeNodeTransitionEvent = (event: TrafficEventItem) => {
  const fromStatus = String(event.payload.from_health_status || '')
  const toStatus = String(event.payload.to_health_status || '')
  const reasonMessage = String(event.payload.reason_message || event.payload.status_reason_message || '')
  const reasonCode = String(event.payload.reason_code || event.payload.status_reason_code || '')
  const fromLabel =
    fromStatus === 'online' || fromStatus === 'degraded' || fromStatus === 'offline'
      ? healthLabelMap[fromStatus]
      : fromStatus || '未知'
  const toLabel =
    toStatus === 'online' || toStatus === 'degraded' || toStatus === 'offline'
      ? healthLabelMap[toStatus]
      : toStatus || '未知'
  const mappedReason =
    reasonCode === 'auth_failed' ||
    reasonCode === 'timeout' ||
    reasonCode === 'traffic_fetch_failed' ||
    reasonCode === 'frame_fetch_failed'
      ? nodeReasonLabelMap[reasonCode]
      : ''
  const reasonLabel = reasonMessage || mappedReason
  return `${fromLabel} -> ${toLabel}${reasonLabel ? ` · ${reasonLabel}` : ''}`
}

const fetchUsers = async () => {
  const res = await authFetch(endpoints.adminUsers)
  if (!res.ok) throw new Error('获取用户列表失败')
  const data = await res.json()
  users.value = (Array.isArray(data) ? data : data?.content ?? []).map(normalizeAdminUser)
}

const fetchCameras = async () => {
  camerasLoading.value = true
  try {
    const res = await authFetch(endpoints.adminCameras)
    if (!res.ok) throw new Error('获取摄像头列表失败')
    const data = await res.json()
    cameras.value = (Array.isArray(data) ? data : data?.content ?? []).map(normalizeCamera)
  } finally {
    camerasLoading.value = false
  }
}

const fetchSettings = async () => {
  const res = await authFetch(endpoints.siteSettings)
  if (!res.ok) throw new Error('获取站点设置失败')
  const data = normalizeSiteSettings(await res.json())
  settings.site_name = data.site_name
  settings.announcement = data.announcement
  settings.logo_url = data.logo_url
  settings.footer_text = data.footer_text
  settings.amap_key = data.amap_key
}

const fetchMonitor = async () => {
  const [r1, r2] = await Promise.all([
    authFetch(endpoints.adminResources),
    authFetch(endpoints.adminNodes),
  ])
  if (r1.ok) resources.value = await r1.json()
  if (r2.ok) nodes.value = await r2.json()
}

const fetchEvents = async () => {
  eventsLoading.value = true
  try {
    const res = await authFetch(`${endpoints.adminEvents}?size=50`)
    if (!res.ok) throw new Error('获取事件日志失败')
    const data = await res.json()
    eventLogs.value = (Array.isArray(data) ? data : data?.content ?? []).map(normalizeTrafficEvent)
  } finally {
    eventsLoading.value = false
  }
}

const retryInit = async () => {
  loading.value = true
  errorText.value = ''
  try {
    await ensureAdmin()
    await Promise.all([fetchUsers(), fetchCameras(), fetchSettings(), fetchEvents()])
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

const openCameraModal = (camera: CameraItem | null) => {
  cameraEditing.value = camera
  if (camera) {
    cameraForm.name = camera.name
    cameraForm.road_name = camera.road_name
    cameraForm.location = camera.location
    cameraForm.stream_url = camera.stream_url
    cameraForm.node_url = camera.node_url
    cameraForm.edge_node_id = camera.edge_node_id
    cameraForm.node_api_key = camera.node_api_key
    cameraForm.latitude = camera.latitude
    cameraForm.longitude = camera.longitude
    cameraForm.enabled = camera.enabled
  } else {
    cameraForm.name = ''
    cameraForm.road_name = ''
    cameraForm.location = ''
    cameraForm.stream_url = ''
    cameraForm.node_url = ''
    cameraForm.edge_node_id = ''
    cameraForm.node_api_key = ''
    cameraForm.latitude = null
    cameraForm.longitude = null
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
    const payload = {
      name: cameraForm.name,
      road_name: cameraForm.road_name || null,
      location: cameraForm.location || null,
      stream_url: cameraForm.stream_url || null,
      node_url: cameraForm.node_url || null,
      edge_node_id: cameraForm.edge_node_id || null,
      node_api_key: cameraForm.node_api_key || null,
      latitude: cameraForm.latitude,
      longitude: cameraForm.longitude,
      enabled: cameraForm.enabled,
    }
    const url = cameraEditing.value ? `${endpoints.adminCameras}/${cameraEditing.value.id}` : endpoints.adminCameras
    const method = cameraEditing.value ? 'PUT' : 'POST'
    const res = await authFetch(url, { method, body: JSON.stringify(payload) })
    if (!res.ok) throw new Error('保存摄像头失败')
    cameraModalVisible.value = false
    await fetchCameras()
    message.success(cameraEditing.value ? '摄像头已更新' : '摄像头已创建')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    cameraSubmitting.value = false
  }
  return false
}

const toggleUserStatus = async (user: AdminUser) => {
  try {
    const res = await authFetch(`${endpoints.adminUsers}/${user.id}/status`, { method: 'PUT' })
    await ensureOk(res, '更新用户状态失败')
    await fetchUsers()
    message.success('用户状态已更新')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '更新用户状态失败')
  }
}

const toggleUserRole = async (user: AdminUser) => {
  try {
    const res = await authFetch(`${endpoints.adminUsers}/${user.id}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role_id: user.role_id === 0 ? 1 : 0 }),
    })
    await ensureOk(res, '更新用户角色失败')
    await fetchUsers()
    message.success('用户角色已更新')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '更新用户角色失败')
  }
}

const toggleCamera = async (camera: CameraItem) => {
  try {
    const res = await authFetch(`${endpoints.adminCameras}/${camera.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: camera.name,
        road_name: camera.road_name || null,
        location: camera.location || null,
        stream_url: camera.stream_url || null,
        node_url: camera.node_url || null,
        edge_node_id: camera.edge_node_id || null,
        node_api_key: camera.node_api_key || null,
        latitude: camera.latitude,
        longitude: camera.longitude,
        enabled: !camera.enabled,
      }),
    })
    await ensureOk(res, camera.enabled ? '禁用摄像头失败' : '启用摄像头失败')
    await fetchCameras()
    message.success(camera.enabled ? '摄像头已禁用' : '摄像头已启用')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '更新摄像头状态失败')
  }
}

const deleteCamera = async (cameraId: number) => {
  try {
    const res = await authFetch(`${endpoints.adminCameras}/${cameraId}`, { method: 'DELETE' })
    await ensureOk(res, '删除摄像头失败')
    await fetchCameras()
    message.success('摄像头已删除')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '删除摄像头失败')
  }
}

const saveSettings = async () => {
  settingsSaving.value = true
  try {
    const res = await authFetch(endpoints.adminSiteSettings, {
      method: 'PUT',
      body: JSON.stringify(settings),
    })
    if (!res.ok) throw new Error('保存站点设置失败')
    const saved = normalizeSiteSettings(await res.json())
    settings.site_name = saved.site_name
    settings.announcement = saved.announcement
    settings.logo_url = saved.logo_url
    settings.footer_text = saved.footer_text
    settings.amap_key = saved.amap_key
    message.success('站点设置已保存')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    settingsSaving.value = false
  }
}

const openNodeConfigModal = async (row: Record<string, unknown>) => {
  const cameraId = Number(row.camera_id || 0)
  if (!cameraId) {
    message.error('该节点未关联 camera_id')
    return
  }
  currentNodeCameraId.value = cameraId
  currentNodeLabel.value = String(row.road_name || row.name || `节点 ${cameraId}`)
  nodeConfigModalVisible.value = true
  const res = await authFetch(endpoints.adminNodeConfig(cameraId))
  if (!res.ok) {
    message.error('读取节点配置失败')
    return
  }
  nodeConfigText.value = JSON.stringify(await res.json(), null, 2)
}

const saveNodeConfig = async () => {
  if (!currentNodeCameraId.value) return false
  let payload: Record<string, unknown>
  try {
    payload = JSON.parse(nodeConfigText.value || '{}')
  } catch {
    message.error('配置 JSON 不合法')
    return false
  }
  nodeConfigSubmitting.value = true
  try {
    const res = await authFetch(endpoints.adminNodeConfig(currentNodeCameraId.value), {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
    if (!res.ok) throw new Error('下发节点配置失败')
    nodeConfigText.value = JSON.stringify(await res.json(), null, 2)
    message.success('节点配置已下发')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '下发失败')
  } finally {
    nodeConfigSubmitting.value = false
  }
  return false
}

const resourceMetrics = computed(() => {
  const memory = (resources.value.memory as Record<string, unknown> | undefined) ?? {}
  const disk = (resources.value.disk as Record<string, unknown> | undefined) ?? {}
  return {
    cpu: Math.round(Number(resources.value.cpu_percent ?? 0)),
    memory: Math.round(Number(memory.percent ?? 0)),
    disk: Math.round(Number(disk.percent ?? 0)),
  }
})

const nodeList = computed<AdminNodeHealth[]>(() => {
  const nodeMap = ((nodes.value.nodes ?? nodes.value) || {}) as Record<string, unknown>
  return Object.entries(nodeMap).map(([name, info]) =>
    normalizeAdminNodeHealth({
      name,
      ...(typeof info === 'object' && info !== null ? (info as Record<string, unknown>) : {}),
    }),
  )
})

const nodeHealthCounts = computed(() => {
  const counts = {
    all: nodeList.value.length,
    issues: 0,
    degraded: 0,
    offline: 0,
    online: 0,
  }
  nodeList.value.forEach((node) => {
    counts[node.health_status] += 1
    if (node.health_status !== 'online') {
      counts.issues += 1
    }
  })
  return counts
})

const displayedNodeList = computed<AdminNodeHealth[]>(() => {
  const filtered = nodeList.value.filter((node) => {
    if (nodeHealthFilter.value === 'all') return true
    if (nodeHealthFilter.value === 'issues') return node.health_status !== 'online'
    return node.health_status === nodeHealthFilter.value
  })

  const sorted = [...filtered]
  sorted.sort((left, right) => {
    if (nodeSortKey.value === 'name') {
      return left.name.localeCompare(right.name, 'zh-CN')
    }
    if (nodeSortKey.value === 'latency_desc') {
      return (right.latency_ms ?? -1) - (left.latency_ms ?? -1)
    }
    if (nodeSortKey.value === 'last_success_desc') {
      return toTimestamp(right.last_success_time) - toTimestamp(left.last_success_time)
    }

    const severityDiff = nodeSeverityRank[left.health_status] - nodeSeverityRank[right.health_status]
    if (severityDiff !== 0) {
      return severityDiff
    }
    const failureDiff = right.consecutive_failures - left.consecutive_failures
    if (failureDiff !== 0) {
      return failureDiff
    }
    const latencyDiff = (right.latency_ms ?? -1) - (left.latency_ms ?? -1)
    if (latencyDiff !== 0) {
      return latencyDiff
    }
    return left.name.localeCompare(right.name, 'zh-CN')
  })

  return sorted
})

const nodeHealthLabelMap: Record<AdminNodeHealth['health_status'], string> = {
  online: '在线',
  degraded: '降级',
  offline: '离线',
}

const nodeHealthTagTypeMap: Record<AdminNodeHealth['health_status'], 'success' | 'warning' | 'error'> = {
  online: 'success',
  degraded: 'warning',
  offline: 'error',
}

const nodeReasonLabelMap: Record<Exclude<AdminNodeHealth['status_reason_code'], null>, string> = {
  auth_failed: '鉴权失败',
  timeout: '请求超时',
  traffic_fetch_failed: '交通拉取失败',
  frame_fetch_failed: '拉帧失败',
}

const nodeReasonTagTypeMap: Record<Exclude<AdminNodeHealth['status_reason_code'], null>, 'error' | 'warning'> = {
  auth_failed: 'error',
  timeout: 'warning',
  traffic_fetch_failed: 'error',
  frame_fetch_failed: 'warning',
}

const nodeStageLabelMap: Record<Exclude<AdminNodeHealth['last_error_stage'], null>, string> = {
  traffic: 'traffic',
  frame: 'frame',
}

const userColumns: DataTableColumns<AdminUser> = [
  { title: '用户名', key: 'username' },
  { title: '邮箱', key: 'email' },
  {
    title: '角色',
    key: 'role_id',
    render: (row) => h(NTag, { type: row.role_id === 0 ? 'warning' : 'info' }, { default: () => (row.role_id === 0 ? '管理员' : '用户') }),
  },
  {
    title: '状态',
    key: 'enabled',
    render: (row) => h(NTag, { type: row.enabled ? 'success' : 'error' }, { default: () => (row.enabled ? '启用' : '禁用') }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: (row) => h('div', { style: 'display:flex;gap:8px' }, [
      h(NButton, { size: 'small', secondary: true, onClick: () => toggleUserRole(row) }, { default: () => '切换角色' }),
      h(NButton, { size: 'small', secondary: true, type: row.enabled ? 'error' : 'success', onClick: () => toggleUserStatus(row) }, { default: () => (row.enabled ? '禁用' : '启用') }),
    ]),
  },
]

const cameraColumns: DataTableColumns<CameraItem> = [
  { title: '名称', key: 'name' },
  { title: '路段', key: 'road_name' },
  { title: '节点 ID', key: 'edge_node_id' },
  { title: '节点地址', key: 'node_url' },
  {
    title: '坐标',
    key: 'coords',
    render: (row) => (row.latitude == null || row.longitude == null ? '—' : `${row.latitude.toFixed(4)}, ${row.longitude.toFixed(4)}`),
  },
  {
    title: '状态',
    key: 'enabled',
    render: (row) => h(NTag, { type: row.enabled ? 'success' : 'error' }, { default: () => (row.enabled ? '启用' : '禁用') }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render: (row) => h('div', { style: 'display:flex;gap:8px' }, [
      h(NButton, { size: 'small', secondary: true, onClick: () => openCameraModal(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', secondary: true, type: row.enabled ? 'error' : 'success', onClick: () => toggleCamera(row) }, { default: () => (row.enabled ? '禁用' : '启用') }),
      h(NPopconfirm, { onPositiveClick: () => deleteCamera(row.id) }, {
        trigger: () => h(NButton, { size: 'small', secondary: true, type: 'error' }, { default: () => '删除' }),
        default: () => '确定删除该摄像头？',
      }),
    ]),
  },
]

const nodeColumns: DataTableColumns<AdminNodeHealth> = [
  { title: '节点', key: 'name' },
  { title: '道路', key: 'road_name' },
  { title: '节点 ID', key: 'edge_node_id' },
  {
    title: '健康状态',
    key: 'health_status',
    render: (row) =>
      h(
        NTag,
        { type: nodeHealthTagTypeMap[row.health_status] },
        { default: () => nodeHealthLabelMap[row.health_status] },
      ),
  },
  {
    title: '原因',
    key: 'status_reason_code',
    width: 280,
    render: (row) => {
      if (!row.status_reason_code) {
        return '—'
      }
      const label = nodeReasonLabelMap[row.status_reason_code]
      const stageLabel = row.last_error_stage ? ` · ${nodeStageLabelMap[row.last_error_stage]}` : ''
      const detail = row.last_error ? `${row.status_reason_message || label}\n${row.last_error}` : (row.status_reason_message || label)
      return h('div', { class: 'node-reason', title: detail }, [
        h(
          NTag,
          { size: 'small', type: nodeReasonTagTypeMap[row.status_reason_code] },
          { default: () => `${label}${stageLabel}` },
        ),
        h('div', { class: 'node-reason__message' }, row.status_reason_message || label),
        row.last_error
          ? h('div', { class: 'node-reason__raw' }, row.last_error)
          : null,
      ])
    },
  },
  {
    title: '延迟',
    key: 'latency_ms',
    render: (row) =>
      row.health_status === 'offline' || row.latency_ms == null ? '—' : `${row.latency_ms} ms`,
  },
  {
    title: '最近成功',
    key: 'last_success_time',
    render: (row) => formatDateTime(row.last_success_time),
  },
  {
    title: '操作',
    key: 'actions',
    render: (row) => h(NButton, { size: 'small', secondary: true, onClick: () => openNodeConfigModal(row) }, { default: () => '查看配置' }),
  },
]

const eventColumns: DataTableColumns<TrafficEventItem> = [
  { title: '道路', key: 'road_name' },
  {
    title: '事件',
    key: 'event_type',
    render: (row) => {
      const isNodeHealthChange = row.event_type === 'node_health_status_changed'
      const tagType = isNodeHealthChange
        ? row.payload.to_health_status === 'offline'
          ? 'error'
          : row.payload.to_health_status === 'degraded'
            ? 'warning'
            : 'success'
        : row.event_type === 'wrong_way_suspected'
          ? 'error'
          : row.event_type === 'illegal_parking_suspected'
            ? 'warning'
            : 'info'
      const label = isNodeHealthChange ? '节点状态变更' : row.event_type
      return h(NTag, { type: tagType }, { default: () => label })
    },
  },
  { title: '级别', key: 'level' },
  {
    title: '时间',
    key: 'start_at',
    render: (row) => formatDateTime(row.start_at),
  },
  {
    title: '摘要',
    key: 'payload',
    render: (row) => {
      if (row.event_type === 'node_health_status_changed') {
        return summarizeNodeTransitionEvent(row)
      }
      const trackId = row.payload.track_id != null ? `track=${row.payload.track_id}` : ''
      const speed = row.payload.speed_kmh != null ? ` speed=${row.payload.speed_kmh}` : ''
      return `${trackId}${speed}`.trim() || '—'
    },
  },
]

watch(tab, async (value) => {
  if (value === 'monitor') {
    await Promise.all([fetchMonitor(), fetchEvents()])
    if (monitorTimer) window.clearInterval(monitorTimer)
    monitorTimer = window.setInterval(() => {
      fetchMonitor()
      fetchEvents()
    }, 5000)
  } else if (monitorTimer) {
    window.clearInterval(monitorTimer)
    monitorTimer = null
  }
})

onMounted(retryInit)

onUnmounted(() => {
  if (monitorTimer) {
    window.clearInterval(monitorTimer)
  }
})
</script>

<style scoped>
.admin-page {
  max-width: 1240px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 12px;
}

.form-hint {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.node-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.node-toolbar__filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.node-reason {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.4;
}

.node-reason__message {
  color: #334155;
  font-size: 13px;
}

.node-reason__raw {
  color: #64748b;
  font-size: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}
</style>
