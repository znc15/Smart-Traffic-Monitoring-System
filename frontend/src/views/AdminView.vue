<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
        <Tabs :default-value="defaultTab">
          <TabsList class="mb-4 flex flex-wrap">
            <TabsTrigger value="site">站点设置</TabsTrigger>
            <TabsTrigger value="ai">AI 配置</TabsTrigger>
            <TabsTrigger value="docs">API 文档</TabsTrigger>
          </TabsList>

          <!-- 站点设置 -->
          <TabsContent value="site">
            <form @submit.prevent="updateSiteSettings" class="space-y-4 max-w-xl">
              <div class="space-y-2">
                <Label for="siteName">站点名称</Label>
                <Input id="siteName" v-model="siteSettings.site_name" />
              </div>
              <div class="space-y-2">
                <Label for="announcement">系统公告</Label>
                <Input id="announcement" v-model="siteSettings.announcement" />
              </div>
              <div class="space-y-2">
                <Label for="footerText">页脚文本</Label>
                <Input id="footerText" v-model="siteSettings.footer_text" />
              </div>
              <div class="space-y-2">
                <Label for="amapKey">高德地图 Key (可选)</Label>
                <Input id="amapKey" v-model="siteSettings.amap_key" placeholder="AMap JS API Key" />
              </div>
              <div class="space-y-2">
                <Label for="amapSecurityJsCode">高德安全密钥 (可选)</Label>
                <Input id="amapSecurityJsCode" v-model="siteSettings.amap_security_js_code" placeholder="AMap Security JS Code" />
              </div>
              <div class="space-y-2">
                <Label for="amapServiceHost">高德代理 Host (可选)</Label>
                <Input id="amapServiceHost" v-model="siteSettings.amap_service_host" placeholder="自定义服务代理，例如 https://api.example.com/_amap" />
              </div>
              <Button type="submit" :disabled="saving">
                {{ saving ? '保存中...' : '保存设置' }}
              </Button>
            </form>
          </TabsContent>

          <!-- AI 配置 -->
          <TabsContent value="ai">
            <form @submit.prevent="updateSiteSettings" class="space-y-4 max-w-xl">
              <div class="space-y-2">
                <Label for="llmProvider">LLM 提供商</Label>
                <Select
                  v-model="siteSettings.llm_provider"
                  :options="[
                    { label: 'OpenAI 兼容（OpenAI / DeepSeek / Ollama）', value: 'openai' },
                    { label: 'Claude（Anthropic）', value: 'claude' },
                  ]"
                  placeholder="选择提供商"
                />
              </div>
              <div class="space-y-2">
                <Label for="llmApiBaseUrl">API Base URL</Label>
                <Input
                  id="llmApiBaseUrl"
                  v-model="siteSettings.llm_api_base_url"
                  placeholder="OpenAI 兼容默认：https://api.openai.com"
                />
                <p class="text-xs text-muted-foreground">
                  使用 OpenAI/DeepSeek 时填写，Claude 无需填写
                </p>
              </div>
              <div class="space-y-2">
                <Label for="llmApiKey">API Key</Label>
                <Input
                  id="llmApiKey"
                  v-model="siteSettings.llm_api_key"
                  type="password"
                  placeholder="sk-..."
                />
              </div>
              <div class="space-y-2">
                <Label for="llmModelName">模型名称</Label>
                <div class="flex items-center gap-2">
                  <div class="flex-1">
                    <template v-if="availableModels.length > 0">
                      <Select
                        v-model="siteSettings.llm_model_name"
                        :options="availableModels.map((m: string) => ({ label: m, value: m }))"
                        placeholder="选择模型"
                      />
                    </template>
                    <template v-else>
                      <Input
                        id="llmModelName"
                        v-model="siteSettings.llm_model_name"
                        placeholder="例如：gpt-4o / deepseek-chat / claude-sonnet-4-20250514"
                      />
                    </template>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    :disabled="fetchingModels"
                    @click="fetchModels"
                  >
                    {{ fetchingModels ? '获取中...' : '获取模型' }}
                  </Button>
                </div>
                <p class="text-xs text-muted-foreground">
                  点击"获取模型"从 /v1/models 加载可用模型列表
                </p>
              </div>
              <div class="space-y-2">
                <Label for="llmTitleModelName">标题生成模型</Label>
                <Input
                  id="llmTitleModelName"
                  v-model="siteSettings.llm_title_model_name"
                  placeholder="留空则使用上方主模型"
                />
                <p class="text-xs text-muted-foreground">
                  用于自动生成对话标题的模型，留空时使用主模型
                </p>
              </div>
              <div class="space-y-2">
                <Label for="llmTitlePrompt">标题生成提示词</Label>
                <textarea
                  id="llmTitlePrompt"
                  v-model="siteSettings.llm_title_prompt"
                  rows="3"
                  class="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="根据以下对话内容生成一个简短的中文标题（不超过20字）"
                />
                <p class="text-xs text-muted-foreground">
                  留空使用默认提示词，控制标题生成的风格和格式
                </p>
              </div>
              <div class="space-y-2">
                <Label>AI 悬浮窗可见页面</Label>
                <p class="text-xs text-muted-foreground">
                  勾选可在哪些页面显示 AI 悬浮窗，全不选则所有页面可见
                </p>
                <div class="flex flex-wrap gap-3 mt-1">
                  <label v-for="page in aiFloatPageOptions" :key="page.value" class="flex items-center gap-1.5 text-sm">
                    <input type="checkbox" :value="page.value" v-model="aiFloatSelectedPages" class="accent-primary" />
                    {{ page.label }}
                  </label>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <Button type="submit" :disabled="saving">
                  {{ saving ? '保存中...' : '保存设置' }}
                </Button>
                <Button type="button" variant="outline" :disabled="testingLlm" @click="testLlmConnection">
                  {{ testingLlm ? '测试中...' : '测试连接' }}
                </Button>
              </div>
            </form>
          </TabsContent>

          <!-- API 接口文档 -->
          <TabsContent value="docs">
            <div v-if="apiDocs" class="space-y-6">
              <Card>
                <CardContent class="p-6 space-y-4">
                  <h3 class="text-2xl font-bold">{{ apiDocs.title }} <Badge variant="secondary">v{{ apiDocs.version }}</Badge></h3>
                  <div class="bg-muted p-4 rounded-lg">
                    <h4 class="font-semibold mb-2">Base URL</h4>
                    <code class="text-sm">{{ apiDocs.base_url }}</code>
                  </div>
                  <div class="bg-muted p-4 rounded-lg">
                    <h4 class="font-semibold mb-2">认证说明 ({{ apiDocs.authentication.type }})</h4>
                    <p class="text-sm text-muted-foreground mb-2">{{ apiDocs.authentication.description }}</p>
                    <ul class="text-sm list-disc list-inside ml-4 space-y-1">
                      <li>API Key Header: <code>{{ apiDocs.authentication.api_key_header }}</code></li>
                      <li>Bearer Token: <code>{{ apiDocs.authentication.bearer_header }}</code></li>
                    </ul>
                  </div>
                </CardContent>
              </Card>

              <div class="space-y-4">
                <h3 class="text-xl font-bold">接口列表</h3>
                <Card v-for="(ep, index) in apiDocs.endpoints" :key="index" class="overflow-hidden">
                  <div
                    class="p-4 flex items-center justify-between cursor-pointer hover:bg-muted/50 transition-colors"
                    @click="toggleEndpoint(ep.path, ep.method)"
                  >
                    <div class="flex items-center gap-4">
                      <span :class="['px-2 py-1 rounded text-xs font-bold border', getMethodColor(ep.method)]">
                        {{ ep.method }}
                      </span>
                      <span class="font-mono text-sm font-semibold">{{ ep.path }}</span>
                      <span class="text-sm text-muted-foreground hidden md:inline-block">{{ ep.description }}</span>
                    </div>
                    <Badge variant="outline">{{ ep.authentication }}</Badge>
                  </div>

                  <div v-if="expandedEndpoints[`${ep.method}-${ep.path}`]" class="p-4 border-t bg-muted/20 space-y-4">
                    <p class="text-sm md:hidden mb-4">{{ ep.description }}</p>

                    <div v-if="ep.parameters && ep.parameters.length > 0">
                      <h5 class="text-sm font-semibold mb-2">参数</h5>
                      <Table class="text-sm">
                        <TableHeader>
                          <TableRow>
                            <TableHead>名称</TableHead>
                            <TableHead>位置</TableHead>
                            <TableHead>必填</TableHead>
                            <TableHead>描述</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          <TableRow v-for="param in ep.parameters" :key="param.name">
                            <TableCell class="font-mono">{{ param.name }}</TableCell>
                            <TableCell>{{ param.type }}</TableCell>
                            <TableCell>
                              <Badge :variant="param.required ? 'default' : 'secondary'">{{ param.required ? '是' : '否' }}</Badge>
                            </TableCell>
                            <TableCell>{{ param.description }}</TableCell>
                          </TableRow>
                        </TableBody>
                      </Table>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
                      <div>
                        <h5 class="text-sm font-semibold mb-2">响应示例</h5>
                        <pre class="bg-zinc-950 text-zinc-50 p-4 rounded-lg text-xs overflow-x-auto"><code>{{ JSON.stringify(ep.response_example, null, 2) }}</code></pre>
                      </div>
                      <div>
                        <h5 class="text-sm font-semibold mb-2">cURL 示例</h5>
                        <pre class="bg-zinc-950 text-zinc-50 p-4 rounded-lg text-xs overflow-x-auto whitespace-pre-wrap break-all"><code>{{ ep.curl_example }}</code></pre>
                      </div>
                    </div>
                  </div>
                </Card>
              </div>
            </div>
            <div v-else class="py-10 text-center text-muted-foreground">
              加载中...
            </div>
          </TabsContent>

        </Tabs>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { normalizeSiteSettings, type SiteSettings } from '../lib/normalize'

const route = useRoute()

const defaultTab = computed(() => {
  if (route.path === '/admin/ai') return 'ai'
  if (route.path === '/admin/docs') return 'docs'
  return 'site'
})

const siteSettings = ref<SiteSettings>(normalizeSiteSettings({}))
const saving = ref(false)
const testingLlm = ref(false)
const fetchingModels = ref(false)
const availableModels = ref<string[]>([])

const aiFloatPageOptions = [
  { label: '交通态势概览', value: '/dashboard' },
  { label: '实时视频监测', value: '/cameras' },
  { label: '节点监控汇聚', value: '/monitoring' },
  { label: '实时状态推送', value: '/map' },
  { label: '历史数据统计', value: '/analytics' },
  { label: '异常事件告警', value: '/alerts' },
  { label: '节点配置管理', value: '/admin' },
  { label: 'AI 智能分析', value: '/ai-assistant' },
]

const aiFloatSelectedPages = ref<string[]>([])

const apiDocs = ref<any>(null)
const expandedEndpoints = ref<Record<string, boolean>>({})

watch(() => siteSettings.value.ai_float_visible_pages, (val) => {
  if (val && val.trim()) {
    aiFloatSelectedPages.value = val.split(',').map((s: string) => s.trim()).filter(Boolean)
  } else {
    aiFloatSelectedPages.value = []
  }
}, { immediate: true })

watch(aiFloatSelectedPages, (val) => {
  siteSettings.value = { ...siteSettings.value, ai_float_visible_pages: val.join(',') }
})

onMounted(() => {
  loadSiteSettings()
  loadApiDocs()
})

async function loadSiteSettings() {
  try {
    const res = await authFetch(endpoints.adminSiteSettingsFull)
    if (res.ok) siteSettings.value = normalizeSiteSettings(await res.json())
  } catch {}
}

async function updateSiteSettings() {
  saving.value = true
  try {
    const res = await authFetch(endpoints.adminSiteSettings, {
      method: 'PUT',
      body: JSON.stringify(siteSettings.value),
    })
    if (res.ok) {
      toast.success('保存成功，刷新页面生效')
    } else {
      toast.error('保存失败')
    }
  } catch {
    toast.error('网络错误')
  } finally {
    saving.value = false
  }
}

async function testLlmConnection() {
  testingLlm.value = true
  try {
    await authFetch(endpoints.adminSiteSettings, {
      method: 'PUT',
      body: JSON.stringify(siteSettings.value),
    })
    const res = await authFetch(endpoints.aiLlmTestConnection, { method: 'POST' })
    if (res.ok) {
      const data = await res.json()
      if (data.success) {
        toast.success('连接测试成功')
      } else {
        toast.error('连接测试失败：' + (data.message || '请检查配置'))
      }
    } else {
      toast.error('测试请求失败')
    }
  } catch {
    toast.error('网络错误')
  } finally {
    testingLlm.value = false
  }
}

async function fetchModels() {
  fetchingModels.value = true
  try {
    await authFetch(endpoints.adminSiteSettings, {
      method: 'PUT',
      body: JSON.stringify(siteSettings.value),
    })
    const res = await authFetch(endpoints.aiLlmModels)
    if (res.ok) {
      const models: string[] = await res.json()
      if (models.length > 0) {
        availableModels.value = models
        toast.success(`获取到 ${models.length} 个模型`)
      } else {
        toast.error('未获取到模型列表，请检查 API Base URL 和 API Key')
      }
    } else {
      toast.error('获取模型列表失败')
    }
  } catch {
    toast.error('网络错误')
  } finally {
    fetchingModels.value = false
  }
}

async function loadApiDocs() {
  try {
    const res = await authFetch(endpoints.apiDocs)
    if (res.ok) {
      apiDocs.value = await res.json()
    }
  } catch {}
}

function toggleEndpoint(path: string, method: string) {
  const key = `${method}-${path}`
  expandedEndpoints.value[key] = !expandedEndpoints.value[key]
}

function getMethodColor(method: string) {
  switch (method.toUpperCase()) {
    case 'GET': return 'bg-blue-100 text-blue-800 border-blue-200'
    case 'POST': return 'bg-green-100 text-green-800 border-green-200'
    case 'PUT': return 'bg-yellow-100 text-yellow-800 border-yellow-200'
    case 'DELETE': return 'bg-red-100 text-red-800 border-red-200'
    default: return 'bg-gray-100 text-gray-800 border-gray-200'
  }
}
</script>