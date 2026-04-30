<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
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
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { normalizeSiteSettings, type SiteSettings } from '../../lib/normalize'

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
</script>