# Developer Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a "Developer Center" page with API Key management and API Documentation viewer using existing backend endpoints.

**Architecture:** A Vue 3 view (`DeveloperCenterView.vue`) containing two tabs (API Keys, API Docs), integrated into the main navigation, fetching data from `/api/v1/admin/api-clients` and `/api/v1/api-docs`.

**Tech Stack:** Vue 3, Tailwind CSS, shadcn-vue, lucide-vue-next.

---

### Task 1: Add API Endpoints to Frontend Configuration

**Files:**
- Modify: `frontend/src/lib/api.ts`

- [ ] **Step 1: Add `apiDocs` to `endpoints`**

Modify `frontend/src/lib/api.ts` to include the `apiDocs` endpoint in the `endpoints` object:

```typescript
export const endpoints = {
  // ... existing endpoints
  adminApiClients: `${API_HTTP_BASE}/admin/api-clients`,
  apiDocs: `${API_HTTP_BASE}/api-docs`,
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/lib/api.ts
git commit -m "feat: add apiDocs endpoint to frontend api configuration"
```

---

### Task 2: Create Developer Center View

**Files:**
- Create: `frontend/src/views/DeveloperCenterView.vue`

- [ ] **Step 1: Scaffold `DeveloperCenterView.vue`**

Create `frontend/src/views/DeveloperCenterView.vue` with the basic Tabs layout:

```vue
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

          <TabsContent value="keys">
            <!-- Placeholder for API Keys -->
            <div>API Keys Component Here</div>
          </TabsContent>

          <TabsContent value="docs">
            <!-- Placeholder for API Docs -->
            <div>API Docs Component Here</div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
</script>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/DeveloperCenterView.vue
git commit -m "feat: scaffold Developer Center view"
```

---

### Task 3: Integrate Developer Center into Routing and Navigation

**Files:**
- Modify: `frontend/src/router.ts`
- Modify: `frontend/src/AppLayout.vue`

- [ ] **Step 1: Add route in `router.ts`**

In `frontend/src/router.ts`, add the new route to the `routes` array (inside the layout with `/` path):

```typescript
import DeveloperCenterView from './views/DeveloperCenterView.vue'

// Inside the children array of the '/' route:
{
  path: 'developer',
  name: 'developer',
  component: DeveloperCenterView,
  meta: { requiresAuth: true }
},
```

- [ ] **Step 2: Add navigation link in `AppLayout.vue`**

In `frontend/src/AppLayout.vue`, add the "开发者中心" link to the sidebar navigation. Also import `Code` icon from `lucide-vue-next`.

```vue
<!-- Import Code icon -->
<script setup lang="ts">
// ...
import { LayoutDashboard, Map, Settings, Users, LogOut, BarChart3, Menu, Code } from 'lucide-vue-next'
// ...
</script>

<!-- In the template navigation section, add the Developer Center link: -->
<router-link
  to="/developer"
  class="flex items-center gap-3 rounded-lg px-3 py-2 text-muted-foreground transition-all hover:text-primary"
  active-class="bg-muted text-primary"
>
  <Code class="h-4 w-4" />
  开发者中心
</router-link>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/router.ts frontend/src/AppLayout.vue
git commit -m "feat: add developer center route and navigation link"
```

---

### Task 4: Implement API Key Management UI

**Files:**
- Modify: `frontend/src/views/DeveloperCenterView.vue`

- [ ] **Step 1: Implement the UI for API Keys**

Update `frontend/src/views/DeveloperCenterView.vue` to fetch API clients and display them in a table, along with Create, Edit, Delete, and Regenerate dialogs.

```vue
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
                      <Badge variant="outline" v-for="ep in client.allowed_endpoints" :key="ep">{{ ep }}</Badge>
                      <span v-if="!client.allowed_endpoints || client.allowed_endpoints.length === 0">-</span>
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

          <!-- API 接口文档 Placeholder -->
          <TabsContent value="docs">
            <div>API Docs Component Here</div>
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
            <Label for="clientEndpoints">允许的端点 (逗号分隔，留空表示全部)</Label>
            <Input id="clientEndpoints" v-model="clientForm.allowedEndpoints" placeholder="/api/v1/maas/*" />
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
import { ref, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'

const clients = ref<any[]>([])
const showClientDialog = ref(false)
const editingClientId = ref<number | null>(null)
const clientForm = ref({
  name: '',
  description: '',
  allowedEndpoints: '',
  rateLimit: 1000
})

const showKeyDialog = ref(false)
const generatedKey = ref('')

onMounted(() => {
  loadClients()
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

function openAddClientDialog() {
  editingClientId.value = null
  clientForm.value = { name: '', description: '', allowedEndpoints: '', rateLimit: 1000 }
  showClientDialog.value = true
}

function editClient(client: any) {
  editingClientId.value = client.id
  clientForm.value = {
    name: client.name,
    description: client.description || '',
    allowedEndpoints: (client.allowed_endpoints || []).join(','),
    rateLimit: client.rate_limit || 1000
  }
  showClientDialog.value = true
}

async function saveClient() {
  try {
    const payload = {
      name: clientForm.value.name,
      description: clientForm.value.description,
      allowedEndpoints: clientForm.value.allowedEndpoints.split(',').map(s => s.trim()).filter(Boolean),
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
</script>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/DeveloperCenterView.vue
git commit -m "feat: implement API Key management UI"
```

---

### Task 5: Implement API Docs UI

**Files:**
- Modify: `frontend/src/views/DeveloperCenterView.vue`

- [ ] **Step 1: Add API Docs rendering logic**

Update `frontend/src/views/DeveloperCenterView.vue` to fetch `/api/v1/api-docs` and display it in the "docs" tab.

First, add imports and states in the script section:
```vue
// ... add to existing script setup
const apiDocs = ref<any>(null)
const expandedEndpoints = ref<Record<string, boolean>>({})

async function loadApiDocs() {
  try {
    const res = await fetch(endpoints.apiDocs)
    if (res.ok) {
      apiDocs.value = await res.json()
    }
  } catch {}
}

onMounted(() => {
  loadClients()
  loadApiDocs()
})

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
```

Then update the `docs` TabsContent in the template:

```vue
<!-- Replace API 接口文档 Placeholder with: -->
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
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/DeveloperCenterView.vue
git commit -m "feat: implement API Docs rendering in Developer Center"
```
