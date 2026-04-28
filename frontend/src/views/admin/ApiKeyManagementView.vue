<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
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
import { authFetch, endpoints } from '../../lib/api'
import { Card, CardContent } from '@/components/ui/card'
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

// API 文档数据用于提取可用端点
const apiDocsEndpoints = ref<string[]>([])

const availableEndpoints = computed(() => apiDocsEndpoints.value)

const isAllEndpointsSelected = computed(() => {
  return availableEndpoints.value.length > 0 &&
         clientForm.value.allowedEndpoints.length === availableEndpoints.value.length
})

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
      const data = await res.json()
      if (data.endpoints) {
        const paths = new Set<string>()
        data.endpoints.forEach((ep: any) => {
          if (ep.path) paths.add(ep.path)
        })
        apiDocsEndpoints.value = Array.from(paths).sort()
      }
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

function toggleAllEndpoints() {
  if (isAllEndpointsSelected.value) {
    clientForm.value.allowedEndpoints = []
  } else {
    clientForm.value.allowedEndpoints = [...availableEndpoints.value]
  }
}
</script>