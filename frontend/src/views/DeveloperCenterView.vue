<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h2 class="text-3xl font-bold tracking-tight">开发者中心</h2>
    </div>
    
    <Card>
      <CardContent class="p-6">
        <Tabs default-value="keys">
          <TabsList class="mb-4">
            <TabsTrigger value="keys">API 密钥管理</TabsTrigger>
            <TabsTrigger value="docs">API 接口文档</TabsTrigger>
          </TabsList>

          <!-- API 密钥管理 -->
          <TabsContent value="keys">
            <div class="flex justify-end mb-4">
              <Button @click="openAddClientDialog">创建 API 密钥</Button>
            </div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名称</TableHead>
                  <TableHead>描述</TableHead>
                  <TableHead>允许端点</TableHead>
                  <TableHead>速率限制</TableHead>
                  <TableHead>最后使用</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="client in clients" :key="client.id">
                  <TableCell>{{ client.name }}</TableCell>
                  <TableCell>{{ client.description || '-' }}</TableCell>
                  <TableCell>
                    <div class="flex flex-wrap gap-1">
                      <Badge variant="outline" v-for="ep in (client.allowed_endpoints || [])" :key="ep">{{ ep }}</Badge>
                      <span v-if="!client.allowed_endpoints || client.allowed_endpoints.length === 0">所有端点</span>
                    </div>
                  </TableCell>
                  <TableCell>{{ client.rate_limit }} 次/日</TableCell>
                  <TableCell>{{ client.last_used_at ? new Date(client.last_used_at).toLocaleString('zh-CN') : '未使用' }}</TableCell>
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <Button variant="ghost" size="sm" @click="regenerateKey(client.id)">重置密钥</Button>
                      <Button variant="ghost" size="sm" @click="editClient(client)">修改</Button>
                      <Button variant="destructive" size="sm" @click="deleteClient(client.id)">删除</Button>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
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

    <!-- Client Dialog -->
    <Dialog :open="showClientDialog" @update:open="showClientDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{{ editingClientId ? '修改 API 密钥' : '创建 API 密钥' }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="saveClient" class="space-y-4">
          <div class="space-y-2">
            <Label for="clientName">名称</Label>
            <Input id="clientName" v-model="clientForm.name" required :disabled="!!editingClientId" />
          </div>
          <div class="space-y-2">
            <Label for="clientDesc">描述</Label>
            <Input id="clientDesc" v-model="clientForm.description" />
          </div>
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <Label>允许的端点</Label>
              <Button type="button" variant="ghost" size="sm" @click="toggleAllEndpoints">
                {{ isAllEndpointsSelected ? '取消全选' : '全选' }}
              </Button>
            </div>
            <div class="border rounded-md p-4 max-h-60 overflow-y-auto space-y-3 bg-muted/20">
              <div v-if="availableEndpoints.length === 0" class="text-sm text-muted-foreground text-center py-2">
                暂无可用的端点
              </div>
              <div v-for="ep in availableEndpoints" :key="ep" class="flex items-center space-x-2">
                <Checkbox 
                  :id="`ep-${ep}`" 
                  :value="ep"
                  :checked="clientForm.allowedEndpoints.includes(ep)"
                  @update:checked="(checked) => {
                    if (checked) {
                      clientForm.allowedEndpoints.push(ep)
                    } else {
                      clientForm.allowedEndpoints = clientForm.allowedEndpoints.filter(e => e !== ep)
                    }
                  }"
                />
                <Label :for="`ep-${ep}`" class="text-sm font-mono cursor-pointer font-normal">{{ ep }}</Label>
              </div>
            </div>
            <p class="text-xs text-muted-foreground">留空表示不允许访问任何端点。如需允许所有，请全选。</p>
          </div>
          <div class="space-y-2">
            <Label for="clientRateLimit">速率限制 (次/日)</Label>
            <Input id="clientRateLimit" type="number" min="1" v-model="clientForm.rateLimit" required />
          </div>
          <DialogFooter class="mt-6">
            <Button type="submit">保存</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- Key Display Dialog -->
    <Dialog :open="showKeyDialog" @update:open="showKeyDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>请妥善保存您的 API 密钥</DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-4">
          <p class="text-sm text-destructive font-medium">注意：出于安全考虑，该密钥仅显示一次。请立即复制并妥善保存。</p>
          <div class="flex items-center gap-2">
            <Input :model-value="generatedKey" readonly class="font-mono" />
            <Button variant="outline" @click="copyKey">复制</Button>
          </div>
        </div>
        <DialogFooter>
          <Button @click="showKeyDialog = false">我已保存</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'

const clients = ref<any[]>([])
const showClientDialog = ref(false)
const editingClientId = ref<number | null>(null)
const clientForm = ref({
  name: '',
  description: '',
  allowedEndpoints: [] as string[],
  rateLimit: 1000
})

const showKeyDialog = ref(false)
const generatedKey = ref('')

const apiDocs = ref<any>(null)
const expandedEndpoints = ref<Record<string, boolean>>({})

const availableEndpoints = computed(() => {
  if (!apiDocs.value || !apiDocs.value.endpoints) return []
  const paths = new Set<string>()
  apiDocs.value.endpoints.forEach((ep: any) => {
    if (ep.path) paths.add(ep.path)
  })
  return Array.from(paths).sort()
})

const isAllEndpointsSelected = computed(() => {
  return availableEndpoints.value.length > 0 && 
         clientForm.value.allowedEndpoints.length === availableEndpoints.value.length
})

function toggleAllEndpoints() {
  if (isAllEndpointsSelected.value) {
    clientForm.value.allowedEndpoints = []
  } else {
    clientForm.value.allowedEndpoints = [...availableEndpoints.value]
  }
}

onMounted(() => {
  loadClients()
  loadApiDocs()
})

async function loadClients() {
  try {
    const res = await authFetch(`${endpoints.adminApiClients}?size=100`)
    if (res.ok) {
      const data = await res.json()
      clients.value = data.content || data || []
    }
  } catch {}
}

async function loadApiDocs() {
  try {
    const res = await authFetch(endpoints.apiDocs)
    if (res.ok) {
      apiDocs.value = await res.json()
    }
  } catch {}
}

function openAddClientDialog() {
  editingClientId.value = null
  clientForm.value = { name: '', description: '', allowedEndpoints: [], rateLimit: 1000 }
  showClientDialog.value = true
}

function editClient(client: any) {
  editingClientId.value = client.id
  clientForm.value = {
    name: client.name,
    description: client.description || '',
    allowedEndpoints: client.allowed_endpoints ? [...client.allowed_endpoints] : [],
    rateLimit: client.rate_limit || 1000
  }
  showClientDialog.value = true
}

async function saveClient() {
  try {
    const payload = {
      name: clientForm.value.name,
      description: clientForm.value.description,
      allowedEndpoints: clientForm.value.allowedEndpoints,
      rateLimit: Number(clientForm.value.rateLimit)
    }

    if (editingClientId.value) {
      const res = await authFetch(`${endpoints.adminApiClients}/${editingClientId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      if (res.ok) {
        toast.success('修改成功')
        showClientDialog.value = false
        loadClients()
      } else {
        toast.error('修改失败')
      }
    } else {
      const res = await authFetch(endpoints.adminApiClients, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (res.ok) {
        const data = await res.json()
        generatedKey.value = data.api_key
        toast.success('添加成功')
        showClientDialog.value = false
        showKeyDialog.value = true
        loadClients()
      } else {
        toast.error('添加失败')
      }
    }
  } catch {
    toast.error('网络错误')
  }
}

async function deleteClient(id: number) {
  if (!confirm('确定要删除该 API 密钥吗？')) return
  try {
    const res = await authFetch(`${endpoints.adminApiClients}/${id}`, { method: 'DELETE' })
    if (res.ok) {
      toast.success('删除成功')
      loadClients()
    } else {
      toast.error('删除失败')
    }
  } catch {
    toast.error('网络错误')
  }
}

async function regenerateKey(id: number) {
  if (!confirm('重新生成密钥将导致旧密钥立即失效。确定要继续吗？')) return
  try {
    const res = await authFetch(`${endpoints.adminApiClients}/${id}/regenerate`, { method: 'POST' })
    if (res.ok) {
      const data = await res.json()
      generatedKey.value = data.api_key
      showKeyDialog.value = true
      loadClients()
    } else {
      toast.error('重置失败')
    }
  } catch {
    toast.error('网络错误')
  }
}

function copyKey() {
  navigator.clipboard.writeText(generatedKey.value)
  toast.success('已复制到剪贴板')
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
