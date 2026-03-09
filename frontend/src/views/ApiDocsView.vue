<template>
  <div class="api-docs-page">
    <!-- Page Header -->
    <div class="docs-header">
      <div class="docs-header-inner">
        <div class="header-brand">
          <n-icon size="32" color="#2080f0" class="brand-icon">
            <CodeSlashOutline />
          </n-icon>
          <div class="brand-text">
            <h1 class="brand-title">{{ docTitle }}</h1>
            <span class="brand-version">v{{ docVersion }}</span>
          </div>
        </div>
        <div class="header-meta">
          <n-tag type="info" :bordered="false">Base URL: {{ baseUrl }}</n-tag>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="docs-body">
      <n-spin :show="loading" description="加载 API 文档...">
        <n-result
          v-if="errorMsg"
          status="error"
          title="加载失败"
          :description="errorMsg"
        >
          <template #footer>
            <n-button @click="fetchDocs">重试</n-button>
          </template>
        </n-result>

        <template v-if="!loading && !errorMsg && docData">
          <!-- Auth Info Card -->
          <n-card class="section-card" title="认证说明">
            <template #header-extra>
              <n-icon color="#f0a020"><LockClosedOutline /></n-icon>
            </template>
            <div class="auth-grid">
              <div class="auth-item">
                <span class="auth-label">认证类型</span>
                <n-tag :bordered="false" type="warning">{{ docData.authentication.type }}</n-tag>
              </div>
              <div class="auth-item">
                <span class="auth-label">API Key 请求头</span>
                <code class="inline-code">{{ docData.authentication.api_key_header }}: &lt;your-key&gt;</code>
              </div>
              <div class="auth-item">
                <span class="auth-label">Bearer Token 请求头</span>
                <code class="inline-code">{{ docData.authentication.bearer_header }}</code>
              </div>
              <div class="auth-item full-width">
                <span class="auth-label">说明</span>
                <span class="auth-desc">{{ docData.authentication.description }}</span>
              </div>
            </div>
          </n-card>

          <!-- HTTP Endpoints -->
          <n-card class="section-card" title="HTTP 端点">
            <template #header-extra>
              <n-tag :bordered="false">{{ docData.endpoints.length }} 个端点</n-tag>
            </template>
            <n-collapse arrow-placement="right">
              <n-collapse-item
                v-for="ep in docData.endpoints"
                :key="ep.path + ep.method"
                :name="ep.path + ep.method"
              >
                <template #header>
                  <div class="endpoint-header">
                    <span :class="['method-badge', `method-${ep.method.toLowerCase()}`]">
                      {{ ep.method }}
                    </span>
                    <code class="endpoint-path">{{ ep.path }}</code>
                    <span class="endpoint-desc">{{ ep.description }}</span>
                    <n-tag
                      size="small"
                      :bordered="false"
                      :type="ep.authentication === '无需认证' ? 'success' : 'warning'"
                      class="auth-badge"
                    >
                      {{ ep.authentication === '无需认证' ? '公开' : '需认证' }}
                    </n-tag>
                  </div>
                </template>

                <div class="endpoint-detail">
                  <!-- Auth info -->
                  <div class="detail-row">
                    <span class="detail-label">认证方式</span>
                    <n-tag size="small" :bordered="false" :type="ep.authentication === '无需认证' ? 'success' : 'warning'">
                      {{ ep.authentication }}
                    </n-tag>
                  </div>

                  <!-- Parameters -->
                  <template v-if="ep.parameters && ep.parameters.length > 0">
                    <div class="detail-section-title">参数</div>
                    <n-data-table
                      :columns="paramColumns"
                      :data="ep.parameters"
                      size="small"
                      :bordered="true"
                      :single-line="false"
                      class="param-table"
                    />
                  </template>
                  <div v-else class="no-params">无参数</div>

                  <!-- Response Example -->
                  <div class="detail-section-title">响应示例</div>
                  <pre class="code-block">{{ formatJson(ep.response_example) }}</pre>

                  <!-- cURL Example -->
                  <div class="detail-section-title">cURL 示例</div>
                  <pre class="code-block code-block--curl">{{ ep.curl_example }}</pre>
                </div>
              </n-collapse-item>
            </n-collapse>
          </n-card>

          <!-- WebSocket Endpoints -->
          <n-card class="section-card" title="WebSocket 端点">
            <template #header-extra>
              <n-tag :bordered="false" type="info">{{ docData.websocket_endpoints.length }} 个端点</n-tag>
            </template>
            <div class="ws-list">
              <div
                v-for="ws in docData.websocket_endpoints"
                :key="ws.path"
                class="ws-item"
              >
                <div class="ws-item-header">
                  <span class="method-badge method-ws">WS</span>
                  <code class="endpoint-path">{{ ws.path }}</code>
                </div>
                <p class="ws-description">{{ ws.description }}</p>
                <div class="ws-auth">
                  <n-icon size="14" color="#f0a020"><LockClosedOutline /></n-icon>
                  <span>{{ ws.authentication }}</span>
                </div>
                <template v-if="ws.path_params && ws.path_params.length > 0">
                  <n-data-table
                    :columns="paramColumns"
                    :data="ws.path_params"
                    size="small"
                    :bordered="true"
                    :single-line="false"
                    class="param-table"
                  />
                </template>
              </div>
            </div>
          </n-card>
        </template>
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { NTag, NIcon } from 'naive-ui'
import { CodeSlashOutline, LockClosedOutline } from '@vicons/ionicons5'
import { authFetch, endpoints } from '../lib/api'

interface ApiParam {
  name: string
  type: string
  description: string
  required: boolean
}

interface ApiEndpoint {
  path: string
  method: string
  description: string
  authentication: string
  parameters: ApiParam[]
  response_example: unknown
  curl_example: string
}

interface WsEndpoint {
  path: string
  description: string
  authentication: string
  path_params: ApiParam[]
}

interface ApiDocsData {
  title: string
  version: string
  base_url: string
  authentication: {
    type: string
    api_key_header: string
    bearer_header: string
    description: string
  }
  endpoints: ApiEndpoint[]
  websocket_endpoints: WsEndpoint[]
}

const loading = ref(true)
const errorMsg = ref('')
const docData = ref<ApiDocsData | null>(null)
const docTitle = ref('智慧交通监控系统 API')
const docVersion = ref('1.0')
const baseUrl = ref('/api/v1')

const paramColumns = [
  {
    title: '参数名',
    key: 'name',
    width: 140,
    render: (row: ApiParam) =>
      h('code', { style: 'font-size: 12px; color: #2080f0' }, row.name),
  },
  {
    title: '位置',
    key: 'type',
    width: 90,
    render: (row: ApiParam) =>
      h(NTag, { size: 'small', bordered: false }, { default: () => row.type }),
  },
  {
    title: '说明',
    key: 'description',
  },
  {
    title: '必填',
    key: 'required',
    width: 70,
    render: (row: ApiParam) =>
      h(
        NTag,
        { size: 'small', bordered: false, type: row.required ? 'error' : 'default' },
        { default: () => (row.required ? '是' : '否') },
      ),
  },
]

function formatJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

async function fetchDocs() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await authFetch(endpoints.apiDocs)
    if (!res.ok) {
      errorMsg.value = `请求失败 (${res.status})`
      return
    }
    const data: ApiDocsData = await res.json()
    docData.value = data
    docTitle.value = data.title || '智慧交通监控系统 API'
    docVersion.value = data.version || '1.0'
    baseUrl.value = data.base_url || '/api/v1'
  } catch (e) {
    errorMsg.value = '无法连接到服务器，请检查网络或后端服务'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDocs()
})
</script>

<style scoped>
.api-docs-page {
  min-height: 100vh;
  background: #f8fafc;
}

/* Header */
.docs-header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  padding: 0 24px;
}

.docs-header-inner {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 0;
  flex-wrap: wrap;
  gap: 12px;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.brand-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
}

.brand-version {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

/* Body */
.docs-body {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  border-radius: 8px;
}

/* Auth grid */
.auth-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.auth-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.auth-item.full-width {
  grid-column: 1 / -1;
}

.auth-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.auth-desc {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
}

.inline-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  background: #f3f4f6;
  color: #d03050;
  padding: 3px 8px;
  border-radius: 4px;
  word-break: break-all;
}

/* Endpoint header */
.endpoint-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.method-badge {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
  letter-spacing: 0.05em;
}

.method-get {
  background: #dcfce7;
  color: #166534;
}

.method-post {
  background: #dbeafe;
  color: #1e40af;
}

.method-put {
  background: #fef3c7;
  color: #92400e;
}

.method-delete {
  background: #fee2e2;
  color: #991b1b;
}

.method-patch {
  background: #fef3c7;
  color: #92400e;
}

.method-ws {
  background: #ede9fe;
  color: #5b21b6;
}

.endpoint-path {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #1f2937;
  flex-shrink: 0;
}

.endpoint-desc {
  font-size: 13px;
  color: #6b7280;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-badge {
  flex-shrink: 0;
}

/* Endpoint detail */
.endpoint-detail {
  padding: 16px 0 8px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  flex-shrink: 0;
}

.detail-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 16px 0 8px;
}

.no-params {
  font-size: 13px;
  color: #9ca3af;
  margin: 8px 0 16px;
}

.param-table {
  margin-bottom: 8px;
}

/* Code blocks */
.code-block {
  background: #1e293b;
  color: #e2e8f0;
  border-radius: 6px;
  padding: 14px 16px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.7;
  overflow-x: auto;
  margin: 0 0 8px;
  white-space: pre;
}

.code-block--curl {
  color: #86efac;
}

/* WebSocket list */
.ws-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ws-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px;
}

.ws-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.ws-description {
  font-size: 13px;
  color: #374151;
  margin: 0 0 10px;
  line-height: 1.6;
}

.ws-auth {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 12px;
}

/* Responsive */
@media (max-width: 640px) {
  .docs-body {
    padding: 16px;
  }

  .auth-grid {
    grid-template-columns: 1fr;
  }

  .endpoint-desc {
    display: none;
  }

  .docs-header {
    padding: 0 16px;
  }
}
</style>
