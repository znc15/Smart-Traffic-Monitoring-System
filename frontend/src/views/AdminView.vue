<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
        <Tabs default-value="users">
          <TabsList class="mb-4">
            <TabsTrigger value="users">用户管理</TabsTrigger>
            <TabsTrigger value="cameras">摄像头</TabsTrigger>
            <TabsTrigger value="site">站点设置</TabsTrigger>
            <TabsTrigger value="health">监控</TabsTrigger>
          </TabsList>

          <!-- 用户管理 -->
          <TabsContent value="users">
            <div class="flex justify-end mb-4">
              <Button @click="openAddUserDialog">
                添加用户
              </Button>
            </div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>用户名</TableHead>
                  <TableHead>邮箱</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="user in paginatedUsers" :key="user.id">
                  <TableCell>{{ user.username }}</TableCell>
                  <TableCell>{{ user.email }}</TableCell>
                  <TableCell>
                    <Badge :variant="user.is_active ? 'default' : 'secondary'">
                      {{ user.is_active ? '正常' : '禁用' }}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{{ user.is_superuser ? '管理员' : '普通用户' }}</Badge>
                  </TableCell>
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <Button variant="ghost" size="sm" @click="toggleUserActive(user)">
                        {{ user.is_active ? '禁用' : '启用' }}
                      </Button>
                      <Button variant="ghost" size="sm" @click="editUser(user)">修改</Button>
                      <Button variant="destructive" size="sm" @click="deleteUser(user.id)">删除</Button>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
            <div class="flex items-center justify-between mt-4">
              <div class="flex items-center gap-2 text-sm text-muted-foreground">
                <span>每页显示</span>
                <div class="w-20">
                  <Select v-model="userPageSize" :options="[{label: '5', value: 5}, {label: '10', value: 10}, {label: '20', value: 20}]" @update:modelValue="userPage = 1" />
                </div>
                <span>条，共 {{ users.length }} 条</span>
              </div>
              <div class="flex items-center gap-4">
                <Button variant="outline" size="sm" :disabled="userPage === 1" @click="userPage--">上一页</Button>
                <span class="text-sm">第 {{ userPage }} 页 / 共 {{ Math.ceil(users.length / userPageSize) || 1 }} 页</span>
                <Button variant="outline" size="sm" :disabled="userPage * userPageSize >= users.length" @click="userPage++">下一页</Button>
              </div>
            </div>
          </TabsContent>

          <!-- 摄像头管理 -->
          <TabsContent value="cameras">
            <div class="flex justify-end mb-4">
              <Button @click="openAddCameraDialog">添加摄像头</Button>
            </div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名称</TableHead>
                  <TableHead>流地址</TableHead>
                  <TableHead>道路名称</TableHead>
                  <TableHead>边缘节点</TableHead>
                  <TableHead>经度</TableHead>
                  <TableHead>纬度</TableHead>
                  <TableHead>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="cam in paginatedCameras" :key="cam.id">
                  <TableCell>{{ cam.name }}</TableCell>
                  <TableCell class="max-w-[200px] truncate" :title="cam.stream_url">{{ cam.stream_url }}</TableCell>
                  <TableCell>{{ cam.road_name }}</TableCell>
                  <TableCell>{{ cam.edge_node_id || '未分配' }}</TableCell>
                  <TableCell>{{ cam.longitude }}</TableCell>
                  <TableCell>{{ cam.latitude }}</TableCell>
                  <TableCell>
                    <div class="flex items-center gap-2">
                      <Button variant="ghost" size="sm" @click="editCamera(cam)">修改</Button>
                      <Button variant="destructive" size="sm" @click="deleteCamera(cam.id)">删除</Button>
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
            <div class="flex items-center justify-between mt-4">
              <div class="flex items-center gap-2 text-sm text-muted-foreground">
                <span>每页显示</span>
                <div class="w-20">
                  <Select v-model="cameraPageSize" :options="[{label: '5', value: 5}, {label: '10', value: 10}, {label: '20', value: 20}]" @update:modelValue="cameraPage = 1" />
                </div>
                <span>条，共 {{ cameras.length }} 条</span>
              </div>
              <div class="flex items-center gap-4">
                <Button variant="outline" size="sm" :disabled="cameraPage === 1" @click="cameraPage--">上一页</Button>
                <span class="text-sm">第 {{ cameraPage }} 页 / 共 {{ Math.ceil(cameras.length / cameraPageSize) || 1 }} 页</span>
                <Button variant="outline" size="sm" :disabled="cameraPage * cameraPageSize >= cameras.length" @click="cameraPage++">下一页</Button>
              </div>
            </div>
          </TabsContent>

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

          <!-- 监控 -->
          <TabsContent value="health">
            <div class="flex items-center gap-4 mb-4">
              <div class="w-48">
                <Select v-model="nodeSortKey" :options="nodeSortOptions" placeholder="排序" />
              </div>
              <Button @click="loadNodes">
                <RefreshCw class="w-4 h-4 mr-2" /> 刷新
              </Button>
            </div>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>节点 ID</TableHead>
                  <TableHead>心跳时间</TableHead>
                  <TableHead>CPU使用率</TableHead>
                  <TableHead>内存使用率</TableHead>
                  <TableHead>状态</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow v-for="node in sortedNodes" :key="node.node_id">
                  <TableCell>{{ node.node_id }}</TableCell>
                  <TableCell>{{ new Date(node.last_heartbeat).toLocaleString('zh-CN') }}</TableCell>
                  <TableCell>{{ typeof node.cpu_usage === 'number' ? node.cpu_usage.toFixed(1) + '%' : '-' }}</TableCell>
                  <TableCell>{{ typeof node.memory_usage === 'number' ? node.memory_usage.toFixed(1) + '%' : '-' }}</TableCell>
                  <TableCell>
                    <Badge :variant="node.online ? 'default' : 'destructive'">
                      {{ node.online ? '在线' : '离线' }}
                    </Badge>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>

    <!-- 用户弹窗 -->
    <Dialog :open="showUserDialog" @update:open="showUserDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{{ editingUserId ? '修改用户' : '添加用户' }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="saveUser" class="space-y-4">
          <div class="space-y-2">
            <Label for="newUsername">用户名</Label>
            <Input id="newUsername" v-model="newUser.username" required />
          </div>
          <div class="space-y-2">
            <Label for="newEmail">邮箱</Label>
            <Input id="newEmail" type="email" v-model="newUser.email" required />
          </div>
          <div class="space-y-2" v-if="!editingUserId">
            <Label for="newPassword">密码</Label>
            <Input id="newPassword" type="password" v-model="newUser.password" required />
          </div>
          <div class="flex items-center space-x-2 mt-4">
            <Switch id="newAdmin" :checked="newUser.is_superuser" @update:checked="newUser.is_superuser = $event" />
            <Label for="newAdmin">管理员权限</Label>
          </div>
          <DialogFooter class="mt-6">
            <Button type="submit">{{ editingUserId ? '保存修改' : '确认添加' }}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>

    <!-- 摄像头弹窗 -->
    <Dialog :open="showCameraDialog" @update:open="showCameraDialog = $event">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{{ editingCameraId ? '修改摄像头' : '添加摄像头' }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="saveCamera" class="space-y-4">
          <div class="space-y-2">
            <Label for="camName">名称</Label>
            <Input id="camName" v-model="newCamera.name" required />
          </div>
          <div class="space-y-2">
            <Label for="camStream">流地址</Label>
            <Input id="camStream" v-model="newCamera.stream_url" required />
          </div>
          <div class="space-y-2">
            <Label for="camRoad">道路名称</Label>
            <Input id="camRoad" v-model="newCamera.road_name" required />
          </div>
          <div class="space-y-2">
            <Label for="camNode">边缘节点 ID</Label>
            <Input id="camNode" v-model="newCamera.edge_node_id" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="camLng">经度</Label>
              <Input id="camLng" type="number" step="any" v-model="newCamera.longitude" />
            </div>
            <div class="space-y-2">
              <Label for="camLat">纬度</Label>
              <Input id="camLat" type="number" step="any" v-model="newCamera.latitude" />
            </div>
          </div>
          <DialogFooter class="mt-6">
            <Button type="submit">保存</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { toast } from 'vue-sonner'
import { RefreshCw } from 'lucide-vue-next'
import { authFetch, endpoints } from '../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { normalizeSiteSettings, type SiteSettings } from '../lib/normalize'

const users = ref<any[]>([])
const cameras = ref<any[]>([])
const nodes = ref<any[]>([])
const siteSettings = ref<SiteSettings>(normalizeSiteSettings({}))
const saving = ref(false)

const userPage = ref(1)
const userPageSize = ref(5)
const cameraPage = ref(1)
const cameraPageSize = ref(5)

const paginatedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize.value
  return users.value.slice(start, start + userPageSize.value)
})

const paginatedCameras = computed(() => {
  const start = (cameraPage.value - 1) * cameraPageSize.value
  return cameras.value.slice(start, start + cameraPageSize.value)
})

const showUserDialog = ref(false)
const editingUserId = ref<number | null>(null)
const newUser = ref({
  username: '',
  email: '',
  password: '',
  is_superuser: false,
})

const showCameraDialog = ref(false)
const editingCameraId = ref<number | null>(null)
const newCamera = ref({
  name: '',
  stream_url: '',
  road_name: '',
  edge_node_id: '',
  longitude: 0,
  latitude: 0,
})

const nodeSortKey = ref('last_heartbeat')
const nodeSortOptions = [
  { label: '心跳时间', value: 'last_heartbeat' },
  { label: 'CPU使用率', value: 'cpu_usage' },
  { label: '内存使用率', value: 'memory_usage' },
]

const sortedNodes = computed(() => {
  return [...nodes.value].sort((a, b) => {
    if (nodeSortKey.value === 'last_heartbeat') {
      return new Date(b.last_heartbeat).getTime() - new Date(a.last_heartbeat).getTime()
    }
    return (b[nodeSortKey.value] || 0) - (a[nodeSortKey.value] || 0)
  })
})

onMounted(() => {
  loadUsers()
  loadCameras()
  loadNodes()
  loadSiteSettings()
})

async function loadUsers() {
  try {
    const res = await authFetch(`${endpoints.adminUsers}?size=100`)
    if (res.ok) {
      const data = await res.json()
      users.value = data.content || data || []
    }
  } catch {}
}

async function loadCameras() {
  try {
    const res = await authFetch(`${endpoints.adminCameras}?size=100`)
    if (res.ok) {
      const data = await res.json()
      cameras.value = data.content || data || []
    }
  } catch {}
}

async function loadNodes() {
  try {
    const res = await authFetch(endpoints.adminNodes)
    if (res.ok) {
      const data = await res.json()
      const rawNodes = data.nodes ? Object.values(data.nodes) : (Array.isArray(data) ? data : [])
      nodes.value = rawNodes.map((n: any) => ({
        ...n,
        node_id: n.edgeNodeId || n.cameraId || n.name || n.roadName || '未知',
        last_heartbeat: n.lastPollTime || n.lastSuccessTime || new Date().toISOString(),
        cpu_usage: n.edgeMetrics?.cpu_usage ?? null,
        memory_usage: n.edgeMetrics?.memory_usage ?? null
      }))
    }
  } catch {}
}

async function loadSiteSettings() {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (res.ok) siteSettings.value = normalizeSiteSettings(await res.json())
  } catch {}
}

async function saveUser() {
  try {
    if (editingUserId.value) {
      const payload = {
        username: newUser.value.username,
        email: newUser.value.email,
        is_superuser: newUser.value.is_superuser
      }
      const res = await authFetch(`${endpoints.adminUsers}/${editingUserId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      if (res.ok) {
        toast.success('修改成功')
        showUserDialog.value = false
        loadUsers()
      } else {
        const err = await res.json().catch(() => null)
        toast.error(err?.detail || '修改失败')
      }
    } else {
      const res = await authFetch(endpoints.adminUsers, {
        method: 'POST',
        body: JSON.stringify(newUser.value),
      })
      if (res.ok) {
        toast.success('添加成功')
        showUserDialog.value = false
        newUser.value = { username: '', email: '', password: '', is_superuser: false }
        loadUsers()
      } else {
        const err = await res.json().catch(() => null)
        toast.error(err?.detail || '添加失败')
      }
    }
  } catch {
    toast.error('网络错误')
  }
}

function openAddUserDialog() {
  editingUserId.value = null
  newUser.value = { username: '', email: '', password: '', is_superuser: false }
  showUserDialog.value = true
}

function editUser(user: any) {
  editingUserId.value = user.id
  newUser.value = {
    username: user.username,
    email: user.email,
    password: '',
    is_superuser: user.is_superuser || user.role_id === 1
  }
  showUserDialog.value = true
}

async function deleteUser(id: number) {
  if (!confirm('确定要删除该用户吗？')) return
  try {
    const res = await authFetch(`${endpoints.adminUsers}/${id}`, { method: 'DELETE' })
    if (res.ok) {
      toast.success('删除成功')
      loadUsers()
    } else {
      const err = await res.json().catch(() => null)
      toast.error(err?.detail || '删除失败')
    }
  } catch {
    toast.error('网络错误')
  }
}

async function toggleUserActive(user: any) {
  try {
    const res = await authFetch(`${endpoints.adminUsers}/${user.id}`, {
      method: 'PUT',
      body: JSON.stringify({ is_active: !user.is_active }),
    })
    if (res.ok) {
      toast.success('更新成功')
      loadUsers()
    }
  } catch {
    toast.error('网络错误')
  }
}

function openAddCameraDialog() {
  editingCameraId.value = null
  newCamera.value = { name: '', stream_url: '', road_name: '', edge_node_id: '', longitude: 0, latitude: 0 }
  showCameraDialog.value = true
}

function editCamera(cam: any) {
  editingCameraId.value = cam.id
  newCamera.value = {
    name: cam.name,
    stream_url: cam.stream_url,
    road_name: cam.road_name,
    edge_node_id: cam.edge_node_id || '',
    longitude: cam.longitude,
    latitude: cam.latitude
  }
  showCameraDialog.value = true
}

async function deleteCamera(id: number) {
  if (!confirm('确定要删除该摄像头吗？')) return
  try {
    const res = await authFetch(`${endpoints.adminCameras}/${id}`, { method: 'DELETE' })
    if (res.ok) {
      toast.success('删除成功')
      loadCameras()
    } else {
      toast.error('删除失败')
    }
  } catch {
    toast.error('网络错误')
  }
}

async function saveCamera() {
  try {
    const payload = {
      ...newCamera.value,
      longitude: Number(newCamera.value.longitude),
      latitude: Number(newCamera.value.latitude),
    }
    
    if (editingCameraId.value) {
      const res = await authFetch(`${endpoints.adminCameras}/${editingCameraId.value}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
      })
      if (res.ok) {
        toast.success('修改成功')
        showCameraDialog.value = false
        loadCameras()
      } else {
        toast.error('修改失败')
      }
    } else {
      const res = await authFetch(endpoints.adminCameras, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      if (res.ok) {
        toast.success('添加成功')
        showCameraDialog.value = false
        loadCameras()
      } else {
        toast.error('添加失败')
      }
    }
  } catch {
    toast.error('网络错误')
  }
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
</script>
