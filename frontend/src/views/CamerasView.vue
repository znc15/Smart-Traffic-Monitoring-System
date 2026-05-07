<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold">节点配置管理</h3>
          <Button @click="openAddCameraDialog">添加摄像头</Button>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>名称</TableHead>
              <TableHead>流地址</TableHead>
              <TableHead>道路名称</TableHead>
              <TableHead>状态</TableHead>
              <TableHead>节点 URL</TableHead>
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
              <TableCell>
                <Badge :variant="cam.enabled !== false ? 'default' : 'secondary'">
                  {{ cam.enabled !== false ? '启用' : '禁用' }}
                </Badge>
              </TableCell>
              <TableCell class="max-w-[150px] truncate" :title="cam.node_url || '未分配'">{{ cam.node_url || '未分配' }}</TableCell>
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
      </CardContent>
    </Card>

    <!-- 摄像头弹窗 -->
    <Dialog :open="showCameraDialog" @update:open="showCameraDialog = $event">
      <DialogContent class="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ editingCameraId ? '修改摄像头' : '添加摄像头' }}</DialogTitle>
        </DialogHeader>
        <form @submit.prevent="saveCamera" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="camName">名称</Label>
              <Input id="camName" v-model="newCamera.name" required />
            </div>
            <div class="space-y-2">
              <Label for="camRoad">道路名称</Label>
              <Input id="camRoad" v-model="newCamera.road_name" required />
            </div>
          </div>
          <div class="space-y-2">
            <Label for="camStream">流地址</Label>
            <Input id="camStream" v-model="newCamera.stream_url" required />
          </div>
          <div class="space-y-2">
            <Label for="camLocation">位置描述</Label>
            <Input id="camLocation" v-model="newCamera.location" placeholder="例如：十字路口东北角" />
          </div>

          <div class="border-t pt-4 mt-4 space-y-4">
            <h4 class="text-sm font-medium text-muted-foreground">边缘节点配置</h4>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label for="camNode">边缘节点 ID</Label>
                <Input id="camNode" v-model="newCamera.edge_node_id" placeholder="选填，关联推理节点" />
              </div>
              <div class="space-y-2">
                <Label for="camNodeUrl">节点 URL</Label>
                <Input id="camNodeUrl" v-model="newCamera.node_url" placeholder="http://192.168.x.x:8000" />
              </div>
            </div>
            <div class="space-y-2">
              <Label for="camNodeKey">节点 API Key</Label>
              <Input id="camNodeKey" v-model="newCamera.node_api_key" placeholder="选填，节点鉴权密钥" />
            </div>
          </div>

          <div class="border-t pt-4 mt-4 space-y-4">
            <h4 class="text-sm font-medium text-muted-foreground">地理坐标</h4>
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

            <div class="space-y-2">
              <div class="flex items-center justify-between">
                <Label>地图选点</Label>
                <span class="text-xs text-muted-foreground" v-if="!effectiveAmapKey">未配置高德地图Key，无法加载地图</span>
                <span class="text-xs text-muted-foreground" v-else>在地图上点击或搜索地址以选择坐标</span>
              </div>
              <div v-if="effectiveAmapKey" class="flex items-center gap-2">
                <Input v-model="searchAddress" placeholder="输入地址进行搜索解析..." @keydown.enter.prevent="geocodeAddress" />
                <Button type="button" variant="secondary" class="shrink-0" @click="geocodeAddress">
                  <Search class="w-4 h-4" />
                </Button>
              </div>
              <div v-if="effectiveAmapKey" class="h-[200px] w-full rounded-md border border-border bg-muted overflow-hidden relative">
                <div ref="mapContainerRef" class="absolute inset-0"></div>
              </div>
            </div>
          </div>
          <div class="flex items-center space-x-2 pt-2">
            <Switch id="camEnabled" :checked="newCamera.enabled" @update:checked="newCamera.enabled = $event" />
            <Label for="camEnabled">启用此摄像头 (在监控列表中显示)</Label>
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
import { ref, onMounted, computed, watch, nextTick, onUnmounted } from 'vue'
import { toast } from 'vue-sonner'
import { Search } from 'lucide-vue-next'
import { authFetch, endpoints, AMAP_KEY, AMAP_SECURITY_JS_CODE, AMAP_SERVICE_HOST } from '../lib/api'
import { ensureAmap } from '../lib/amap'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { Select } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'

const cameras = ref<any[]>([])
const cameraPage = ref(1)
const cameraPageSize = ref(5)

const paginatedCameras = computed(() => {
  const start = (cameraPage.value - 1) * cameraPageSize.value
  return cameras.value.slice(start, start + cameraPageSize.value)
})

const showCameraDialog = ref(false)
const editingCameraId = ref<number | null>(null)
const newCamera = ref({
  name: '',
  stream_url: '',
  road_name: '',
  location: '',
  edge_node_id: '',
  node_url: '',
  node_api_key: '',
  enabled: true,
  longitude: 0,
  latitude: 0,
})

const searchAddress = ref('')
const mapContainerRef = ref<HTMLDivElement | null>(null)
let mapInstance: any = null
let mapMarker: any = null

const effectiveAmapKey = computed(() => String(AMAP_KEY || '').trim())
const effectiveAmapSecurityJsCode = computed(() => String(AMAP_SECURITY_JS_CODE || '').trim())
const effectiveAmapServiceHost = computed(() => String(AMAP_SERVICE_HOST || '').trim())

const destroyMap = () => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  mapMarker = null
}

const initMap = async () => {
  if (!effectiveAmapKey.value || !mapContainerRef.value) return
  try {
    const AMap = await ensureAmap({
      key: effectiveAmapKey.value,
      securityJsCode: effectiveAmapSecurityJsCode.value,
      serviceHost: effectiveAmapServiceHost.value,
    })
    if (!mapContainerRef.value) return

    const defaultCenter = [116.397428, 39.90923]
    const initialLng = Number(newCamera.value.longitude) || defaultCenter[0]
    const initialLat = Number(newCamera.value.latitude) || defaultCenter[1]

    mapInstance = new AMap.Map(mapContainerRef.value, {
      viewMode: '3D',
      zoom: 13,
      center: [initialLng, initialLat],
    })

    mapMarker = new AMap.Marker({
      position: [initialLng, initialLat],
      draggable: true,
      cursor: 'move',
    })

    if (newCamera.value.longitude && newCamera.value.latitude) {
      mapMarker.setMap(mapInstance)
    }

    mapInstance.on('click', (e: any) => {
      const lng = e.lnglat.getLng()
      const lat = e.lnglat.getLat()
      newCamera.value.longitude = Number(lng.toFixed(6))
      newCamera.value.latitude = Number(lat.toFixed(6))
      updateMarkerPosition()
    })

    mapMarker.on('dragend', (e: any) => {
      const lng = e.lnglat.getLng()
      const lat = e.lnglat.getLat()
      newCamera.value.longitude = Number(lng.toFixed(6))
      newCamera.value.latitude = Number(lat.toFixed(6))
    })
  } catch (error) {
    console.error('地图初始化失败', error)
  }
}

const geocodeAddress = async () => {
  if (!searchAddress.value || !effectiveAmapKey.value) return
  try {
    const AMap = await ensureAmap({
      key: effectiveAmapKey.value,
      securityJsCode: effectiveAmapSecurityJsCode.value,
      serviceHost: effectiveAmapServiceHost.value,
    })

    AMap.plugin('AMap.Geocoder', () => {
      const geocoder = new AMap.Geocoder()
      geocoder.getLocation(searchAddress.value, (status: string, result: any) => {
        if (status === 'complete' && result.info === 'OK') {
          const location = result.geocodes[0].location
          newCamera.value.longitude = Number(location.lng.toFixed(6))
          newCamera.value.latitude = Number(location.lat.toFixed(6))
          if (mapInstance) {
            mapInstance.setCenter([location.lng, location.lat])
          }
          toast.success('地址解析成功')
        } else {
          toast.error('地址解析失败或未找到相关位置')
        }
      })
    })
  } catch {
    toast.error('加载地理编码服务失败')
  }
}

const updateMarkerPosition = () => {
  if (!mapInstance || !mapMarker) return
  const lng = Number(newCamera.value.longitude)
  const lat = Number(newCamera.value.latitude)
  if (lng && lat) {
    mapMarker.setPosition([lng, lat])
    mapMarker.setMap(mapInstance)
  } else {
    mapMarker.setMap(null)
  }
}

watch(
  () => [newCamera.value.longitude, newCamera.value.latitude],
  () => updateMarkerPosition(),
)

watch(showCameraDialog, async (val) => {
  if (val) {
    searchAddress.value = ''
    await nextTick()
    setTimeout(() => initMap(), 100)
  } else {
    destroyMap()
  }
})

onUnmounted(() => destroyMap())

onMounted(() => {
  loadCameras()
})

async function loadCameras() {
  try {
    const res = await authFetch(`${endpoints.adminCameras}?size=100`)
    if (res.ok) {
      const data = await res.json()
      cameras.value = data.content || data || []
    }
  } catch {}
}

function openAddCameraDialog() {
  editingCameraId.value = null
  newCamera.value = {
    name: '', stream_url: '', road_name: '', location: '',
    edge_node_id: '', node_url: '', node_api_key: '',
    enabled: true, longitude: 0, latitude: 0,
  }
  showCameraDialog.value = true
}

function editCamera(cam: any) {
  editingCameraId.value = cam.id
  newCamera.value = {
    name: cam.name, stream_url: cam.stream_url, road_name: cam.road_name,
    location: cam.location || '', edge_node_id: cam.edge_node_id || '',
    node_url: cam.node_url || '', node_api_key: cam.node_api_key || '',
    enabled: cam.enabled !== false,
    longitude: cam.longitude, latitude: cam.latitude,
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
</script>
