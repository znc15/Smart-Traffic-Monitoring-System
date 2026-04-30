<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
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
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { authFetch, endpoints } from '../../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'

const apiDocs = ref<any>(null)
const expandedEndpoints = ref<Record<string, boolean>>({})

onMounted(() => {
  loadApiDocs()
})

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