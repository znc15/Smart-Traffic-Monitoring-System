<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
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
              <TableHead>手机号</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>角色</TableHead>
              <TableHead>操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow v-for="user in paginatedUsers" :key="user.id">
              <TableCell>{{ user.username }}</TableCell>
              <TableCell>{{ user.email }}</TableCell>
              <TableCell>{{ user.phone_number || '-' }}</TableCell>
              <TableCell>
                <Badge :variant="user.enabled ? 'default' : 'secondary'">
                  {{ user.enabled ? '正常' : '禁用' }}
                </Badge>
              </TableCell>
              <TableCell>
                <Badge variant="outline">
                  {{ user.is_superuser ? '管理员' : '普通用户' }}
                </Badge>
              </TableCell>
              <TableCell>
                <div class="flex items-center gap-2">
                  <Button variant="ghost" size="sm" @click="toggleUserActive(user)">
                    {{ user.enabled ? '禁用' : '启用' }}
                  </Button>
                  <Button variant="ghost" size="sm" @click="editUser(user)">
                    修改
                  </Button>
                  <Button variant="destructive" size="sm" @click="deleteUser(user.id)">
                    删除
                  </Button>
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
            <Button variant="outline" size="sm" :disabled="userPage === 1" @click="userPage--">
              上一页
            </Button>
            <span class="text-sm">
              第 {{ userPage }} 页 / 共 {{ Math.ceil(users.length / userPageSize) || 1 }} 页
            </span>
            <Button variant="outline" size="sm" :disabled="userPage * userPageSize >= users.length" @click="userPage++">
              下一页
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 用户弹窗 -->
    <Dialog :open="showUserDialog" @update:open="showUserDialog = $event">
      <DialogContent class="max-h-[90vh] overflow-y-auto">
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
          <div class="space-y-2">
            <Label for="newPhone">手机号</Label>
            <Input id="newPhone" type="tel" v-model="newUser.phone_number" placeholder="选填，例如：13800000000" />
          </div>
          <div class="space-y-2">
            <Label for="newPassword">密码</Label>
            <Input id="newPassword" type="password" v-model="newUser.password" :placeholder="editingUserId ? '留空表示不修改' : '至少8位'" :required="!editingUserId" minlength="8" />
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
import { Switch } from '@/components/ui/switch'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'

const users = ref<any[]>([])
const userPage = ref(1)
const userPageSize = ref(5)
const showUserDialog = ref(false)
const editingUserId = ref<number | null>(null)
const newUser = ref({
  username: '',
  email: '',
  phone_number: '',
  password: '',
  is_superuser: false,
})

const paginatedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize.value
  return users.value.slice(start, start + userPageSize.value)
})

onMounted(() => {
  loadUsers()
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

async function saveUser() {
  try {
    if (editingUserId.value) {
      const payload = {
        username: newUser.value.username,
        email: newUser.value.email,
        phone_number: newUser.value.phone_number || null,
        password: newUser.value.password || null,
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
        newUser.value = { username: '', email: '', phone_number: '', password: '', is_superuser: false }
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
  newUser.value = { username: '', email: '', phone_number: '', password: '', is_superuser: false }
  showUserDialog.value = true
}

function editUser(user: any) {
  editingUserId.value = user.id
  newUser.value = {
    username: user.username,
    email: user.email,
    phone_number: user.phone_number || user.phoneNumber || '',
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
    const res = await authFetch(`${endpoints.adminUsers}/${user.id}/status`, {
      method: 'PUT',
    })
    if (res.ok) {
      toast.success('状态更新成功')
      loadUsers()
    } else {
      const err = await res.json().catch(() => null)
      toast.error(err?.detail || '状态更新失败')
    }
  } catch {
    toast.error('网络错误')
  }
}
</script>