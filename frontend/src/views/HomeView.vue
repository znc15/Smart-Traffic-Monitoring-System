<template>
  <section class="min-h-screen flex flex-col bg-background">
    <!-- 顶部导航 -->
    <header class="h-16 border-b border-border bg-card/50 backdrop-blur flex items-center justify-between px-6 shrink-0">
      <div class="flex items-center gap-3">
        <Car class="h-6 w-6 text-primary" />
        <span class="font-bold text-lg">{{ siteName }}</span>
      </div>
      <div class="flex items-center gap-3">
        <Button variant="ghost" size="icon" @click="toggleDark()">
          <Sun v-if="isDark" class="h-5 w-5" />
          <Moon v-else class="h-5 w-5" />
        </Button>
        <Button v-if="loggedIn" @click="$router.push('/dashboard')">
          进入控制台
        </Button>
        <Button v-else @click="$router.push('/login')">
          登录系统
        </Button>
      </div>
    </header>

    <!-- 主内容 -->
    <main class="flex-1 flex items-center justify-center p-8">
      <div class="max-w-5xl w-full space-y-12">
        <!-- Hero -->
        <div class="text-center space-y-4">
          <h1 class="text-4xl sm:text-5xl font-bold tracking-tight">
            {{ siteName }}
          </h1>
          <p class="text-lg text-muted-foreground max-w-2xl mx-auto">
            基于 AI 的智能交通监控平台，实时路况感知、拥堵分析、智能建议，助力城市交通高效运行。
          </p>
        </div>

        <!-- 特性卡片 -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <Card v-for="feat in features" :key="feat.title" class="hover:shadow-lg transition-shadow">
            <CardHeader>
              <div class="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center mb-2">
                <component :is="feat.icon" class="h-5 w-5 text-primary" />
              </div>
              <CardTitle class="text-base">{{ feat.title }}</CardTitle>
            </CardHeader>
            <CardContent>
              <p class="text-sm text-muted-foreground">{{ feat.desc }}</p>
            </CardContent>
          </Card>
        </div>

        <!-- 公告 -->
        <Alert v-if="announcement" variant="default" class="max-w-2xl mx-auto">
          <Info class="h-4 w-4" />
          <AlertDescription class="ml-2">{{ announcement }}</AlertDescription>
        </Alert>
      </div>
    </main>

    <!-- 底部 -->
    <footer v-if="footerText" class="text-center text-sm text-muted-foreground py-4 border-t border-border">
      {{ footerText }}
    </footer>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useDark, useToggle } from '@vueuse/core'
import {
  Car, Sun, Moon, Activity, MapPin, BarChart, Bot, Shield, Radio, Video,
  Info,
} from 'lucide-vue-next'
import { getToken, endpoints } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

const isDark = useDark()
const toggleDark = useToggle(isDark)

const loggedIn = !!getToken()
const siteName = ref('智能交通监控系统')
const footerText = ref('')
const announcement = ref('')

const features = [
  { title: '实时监控', desc: '多路段视频流接入，车流/人流/车速实时采集，毫秒级数据更新。', icon: Video },
  { title: '实时状态推送', desc: '高德地图集成，路段状态可视化，拥堵热力图一目了然。', icon: MapPin },
  { title: '历史数据统计', desc: '历史趋势图表，自定义时间范围，流量对比与异常检测。', icon: BarChart },
  { title: 'AI 智能分析', desc: '接入大语言模型，智能分析路况、提供分流与优化建议。', icon: Bot },
  { title: '节点监控汇聚', desc: '支持远程节点接入，边缘端 AI 推理，降低带宽消耗。', icon: Radio },
  { title: '开放 API', desc: 'RESTful API + API Key 认证，便于二次开发和数据集成。', icon: Shield },
]

onMounted(async () => {
  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) return
    const settings = normalizeSiteSettings(await res.json())
    siteName.value = settings.site_name || '智能交通监控系统'
    footerText.value = settings.footer_text || ''
    announcement.value = settings.announcement || ''
  } catch {}
})
</script>
