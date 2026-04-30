<template>
  <div class="space-y-6">
    <Card>
      <CardContent class="p-6">
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
      </CardContent>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../../lib/api'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { normalizeSiteSettings, type SiteSettings } from '../../lib/normalize'

const siteSettings = ref<SiteSettings>(normalizeSiteSettings({}))
const saving = ref(false)

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
</script>