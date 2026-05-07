<template>
  <section class="min-h-screen bg-background text-foreground">
    <header class="sticky top-0 z-30 border-b border-amber-500/25 bg-gradient-to-r from-[#1a0f02]/90 via-[#2a1403]/85 to-[#130a01]/90 backdrop-blur">
      <div class="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Car class="h-5 w-5" />
          </div>
          <div class="min-w-0">
            <p class="truncate text-base font-semibold">{{ siteName }}</p>
            <p class="hidden text-xs font-medium text-foreground sm:block">Traffic Intelligence Platform</p>
          </div>
        </div>

        <nav class="hidden items-center gap-6 text-sm font-medium text-foreground lg:flex">
          <a href="#intro" class="transition-colors hover:text-foreground">项目介绍</a>
          <a href="#capabilities" class="transition-colors hover:text-foreground">平台能力</a>
          <a href="#workflow" class="transition-colors hover:text-foreground">运行流程</a>
        </nav>

        <div class="flex items-center gap-2 sm:gap-3">
          <Button variant="ghost" size="icon" title="切换深色模式" @click="toggleDark()">
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
      </div>
    </header>

    <main>
      <section class="relative isolate overflow-hidden">
        <div
          class="absolute inset-0 bg-cover bg-center transition-all duration-700"
          :style="{ backgroundImage: `url(${activeSlide.image})` }"
        />
        <div class="absolute inset-0 bg-black/60" />
        <div class="absolute inset-x-0 bottom-0 h-32 bg-gradient-to-t from-background to-transparent" />

        <div class="relative mx-auto flex min-h-[calc(100vh-4rem)] max-w-7xl items-center px-4 py-16 sm:px-6 lg:px-8">
          <div class="max-w-3xl space-y-7 text-white">
            <div class="inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-sm backdrop-blur">
              <Activity class="h-4 w-4 text-emerald-300" />
              <span>{{ activeSlide.eyebrow }}</span>
            </div>
            <div class="space-y-5">
              <h1 class="text-4xl font-bold sm:text-5xl lg:text-6xl">
                {{ activeSlide.title }}
              </h1>
              <p class="max-w-2xl text-base leading-8 text-white/80 sm:text-lg">
                {{ activeSlide.description }}
              </p>
            </div>
            <div class="flex flex-col gap-3 sm:flex-row">
              <Button size="lg" class="w-full sm:w-auto" @click="$router.push(loggedIn ? '/dashboard' : '/login')">
                {{ loggedIn ? '查看实时态势' : '开始使用平台' }}
                <ArrowRight class="ml-2 h-4 w-4" />
              </Button>
              <Button size="lg" variant="secondary" class="w-full bg-white/15 text-white hover:bg-white/25 sm:w-auto" @click="$router.push('/ai-assistant')">
                AI 智能分析
              </Button>
            </div>
            <div class="grid max-w-xl grid-cols-3 gap-3 pt-2">
              <div v-for="metric in heroMetrics" :key="metric.label" class="rounded-lg border border-white/15 bg-white/10 p-3 backdrop-blur">
                <p class="text-xl font-semibold">{{ metric.value }}</p>
                <p class="mt-1 text-xs text-white/70">{{ metric.label }}</p>
              </div>
            </div>
          </div>

          <div class="absolute bottom-8 left-1/2 flex -translate-x-1/2 items-center gap-3">
            <Button variant="secondary" size="icon" class="h-9 w-9 bg-white/15 text-white hover:bg-white/25" title="上一张" @click="previousSlide">
              <ChevronLeft class="h-4 w-4" />
            </Button>
            <button
              v-for="(slide, index) in slides"
              :key="slide.title"
              class="h-2.5 rounded-full transition-all"
              :class="index === activeSlideIndex ? 'w-8 bg-white' : 'w-2.5 bg-white/45 hover:bg-white/70'"
              :title="`切换到${slide.title}`"
              @click="goToSlide(index)"
            />
            <Button variant="secondary" size="icon" class="h-9 w-9 bg-white/15 text-white hover:bg-white/25" title="下一张" @click="nextSlide">
              <ChevronRight class="h-4 w-4" />
            </Button>
          </div>
        </div>
      </section>

      <section v-if="announcement" class="border-b border-border bg-muted/40 px-4 py-4 sm:px-6 lg:px-8">
        <Alert variant="default" class="mx-auto max-w-7xl bg-background">
          <Info class="h-4 w-4" />
          <AlertDescription class="ml-2">{{ announcement }}</AlertDescription>
        </Alert>
      </section>

      <section id="intro" class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="grid gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-start">
          <div class="space-y-5">
            <p class="text-sm font-semibold text-primary">项目介绍</p>
            <h2 class="text-3xl font-bold sm:text-4xl">面向城市道路的智能交通监测平台</h2>
            <p class="text-base font-medium leading-8 text-foreground">
              平台把道路摄像机、边缘推理节点、中心 API、地图态势和 AI 分析助手连接到统一工作台。它适合交通管理、园区道路、城市路口和项目演示场景，帮助运维人员从“看到异常”走向“理解原因并快速处置”。
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <Card v-for="item in overviewCards" :key="item.title" class="border-muted/70">
              <CardHeader>
                <div class="mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
                  <component :is="item.icon" class="h-5 w-5" />
                </div>
                <CardTitle class="text-base">{{ item.title }}</CardTitle>
              </CardHeader>
              <CardContent>
                <p class="text-sm font-medium leading-6 text-foreground">{{ item.description }}</p>
              </CardContent>
            </Card>
          </div>
        </div>
      </section>

      <section id="capabilities" class="border-y border-border bg-muted/35">
        <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
          <div class="grid gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:items-start">
            <div class="space-y-5">
              <p class="text-sm font-semibold text-primary">平台能力</p>
              <h2 class="text-3xl font-bold">围绕态势、告警与数据复盘</h2>
              <p class="text-base font-medium leading-8 text-foreground">
                聚焦“看得见、看得懂、能跟进”的监测闭环，用更少的步骤完成道路态势展示与事件处置。
              </p>
            </div>

            <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <Card v-for="feat in features" :key="feat.title" class="border-muted/70 transition-shadow hover:shadow-lg">
                <CardHeader>
                  <div class="mb-3 flex h-11 w-11 items-center justify-center rounded-lg" :class="feat.tone">
                    <component :is="feat.icon" class="h-5 w-5" />
                  </div>
                  <CardTitle class="text-base font-semibold">{{ feat.title }}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p class="text-sm font-medium leading-6 text-foreground">{{ feat.desc }}</p>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </section>

      <section id="workflow" class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="grid gap-10 lg:grid-cols-[360px_1fr]">
          <div class="space-y-4">
            <p class="text-sm font-semibold text-primary">运行流程</p>
            <h2 class="text-3xl font-bold">从路口数据到处置建议</h2>
            <p class="text-sm font-medium leading-7 text-foreground">
              系统围绕采集、推理、汇聚、研判和复盘组织工作流，让实时监控和历史治理共用同一份交通数据。
            </p>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <div v-for="step in workflow" :key="step.title" class="rounded-lg border border-border bg-card p-5">
              <div class="mb-4 flex items-center gap-3">
                <div class="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground">
                  {{ step.index }}
                </div>
                <h3 class="font-semibold">{{ step.title }}</h3>
              </div>
              <p class="text-sm font-medium leading-6 text-foreground">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="rounded-lg border border-cyan-500/20 bg-[#050815] px-6 py-10 text-white sm:px-10">
          <div class="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div class="max-w-2xl space-y-3">
              <h2 class="text-2xl font-bold">准备查看实时交通态势？</h2>
              <p class="text-sm font-medium leading-6 text-white/85">
                登录后可以进入控制台查看摄像机、节点、地图、告警、统计报表和 AI 路况分析。
              </p>
            </div>
            <Button variant="secondary" size="lg" class="w-full sm:w-auto" @click="$router.push(loggedIn ? '/dashboard' : '/login')">
              {{ loggedIn ? '进入控制台' : '登录系统' }}
              <ArrowRight class="ml-2 h-4 w-4" />
            </Button>
          </div>
        </div>

        <div class="mt-8 space-y-4 rounded-lg border border-border bg-background p-6 sm:p-8">
          <div class="space-y-2">
            <p class="text-sm font-semibold text-primary">态势广告</p>
            <h3 class="text-xl font-bold">一屏掌握全城交通</h3>
            <p class="text-sm font-medium leading-6 text-foreground">
              实时车流、拥堵与告警，打开就能看见变化
            </p>
          </div>

          <div class="grid gap-5 lg:grid-cols-3">
            <div v-for="card in promoCards" :key="card.title" class="overflow-hidden rounded-lg border border-border bg-card">
              <img :src="card.image" :alt="card.title" class="h-40 w-full object-cover sm:h-44" loading="lazy" />
              <div class="space-y-2 p-5">
                <h4 class="text-base font-bold">{{ card.title }}</h4>
                <p class="text-sm font-medium leading-6 text-foreground">{{ card.desc }}</p>
                <Button class="w-full" @click="$router.push(loggedIn ? '/dashboard' : '/login')">
                  {{ loggedIn ? '立即查看态势' : '登录后查看态势' }}
                  <ArrowRight class="ml-2 h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <footer class="border-t border-amber-500/25 bg-gradient-to-r from-[#130a01] via-[#2a1403] to-[#130a01]">
      <div class="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-[1.2fr_0.8fr_0.8fr_0.8fr] lg:px-8">
        <div class="space-y-4">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Car class="h-5 w-5" />
            </div>
            <span class="font-bold">{{ siteName }}</span>
          </div>
          <p class="max-w-lg text-sm font-medium leading-6 text-foreground">
            {{ footerText || 'Smart Traffic Monitoring System，为道路监测、边缘推理、告警联动和交通治理提供统一入口。' }}
          </p>
        </div>
        <div>
          <h3 class="mb-4 text-sm font-bold">核心模块</h3>
          <div class="space-y-2 text-sm font-medium text-foreground">
            <p>实时视频监测</p>
            <p>节点监控汇聚</p>
            <p>历史数据统计</p>
            <p>异常事件告警</p>
          </div>
        </div>
        <div>
          <h3 class="mb-4 text-sm font-bold">快速入口</h3>
          <div class="space-y-2 text-sm font-medium text-foreground">
            <button class="block transition-colors hover:text-foreground" @click="$router.push('/dashboard')">交通态势概览</button>
            <button class="block transition-colors hover:text-foreground" @click="$router.push('/map')">实时状态推送</button>
            <button class="block transition-colors hover:text-foreground" @click="$router.push('/ai-assistant')">AI 智能分析</button>
          </div>
        </div>
        <div>
          <h3 class="mb-4 text-sm font-bold">友情链接</h3>
          <div class="space-y-2 text-sm font-medium text-foreground">
            <a
              class="block transition-colors hover:text-primary"
              href="https://www.gkd.edu.cn/jwc/"
              target="_blank"
              rel="noopener noreferrer"
            >
              广州科技职业技术大学
            </a>
            <a class="block transition-colors hover:text-primary" href="https://github.com" target="_blank" rel="noopener noreferrer">
              GitHub
            </a>
          </div>
        </div>
      </div>
      <div class="border-t border-amber-500/25 px-4 py-4 text-center text-xs font-medium text-foreground">
        &copy; {{ currentYear }} Smart Traffic Monitoring System. All rights reserved.
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useDark, useToggle } from '@vueuse/core'
import {
  Activity,
  ArrowRight,
  BarChart,
  BellRing,
  Car,
  ChevronLeft,
  ChevronRight,
  Cpu,
  Database,
  Info,
  Layers,
  MapPin,
  Monitor,
  Moon,
  Sun,
  Video,
} from 'lucide-vue-next'
import { getToken, endpoints } from '../lib/api'
import { normalizeSiteSettings } from '../lib/normalize'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

type IconComponent = typeof Activity

interface HeroSlide {
  eyebrow: string
  title: string
  description: string
  image: string
}

interface IconCard {
  title: string
  description: string
  icon: IconComponent
}

interface FeatureCard {
  title: string
  desc: string
  icon: IconComponent
  tone: string
}

interface WorkflowStep {
  index: string
  title: string
  description: string
}

interface PromoCard {
  title: string
  desc: string
  image: string
}

const isDark = useDark()
const toggleDark = useToggle(isDark)

const loggedIn = !!getToken()
const siteName = ref('智能交通监控系统')
const footerText = ref('')
const announcement = ref('')
const activeSlideIndex = ref(0)
const currentYear = new Date().getFullYear()
let carouselTimer: number | undefined

const slides: HeroSlide[] = [
  {
    eyebrow: '实时态势感知',
    title: '让每一条道路的状态都清晰可见',
    description: '汇聚视频、节点遥测、地图状态和异常告警，在一个入口掌握车流、拥堵、事件和处置进展。',
    image: 'https://images.unsplash.com/photo-1519501025264-65ba15a82390?auto=format&fit=crop&w=1800&q=80',
  },
  {
    eyebrow: '边缘协同推理',
    title: '把 AI 推理前移到道路现场',
    description: '边缘节点完成视频识别和数据压缩，中心服务统一编排、存储和分析，兼顾实时性、带宽成本与可维护性。',
    image: 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1800&q=80',
  },
  {
    eyebrow: '智能研判闭环',
    title: '从告警发现到治理建议一步到位',
    description: 'AI 助手结合道路上下文、历史趋势和实时事件，生成可执行的分流、巡检和资源调度建议。',
    image: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?auto=format&fit=crop&w=1800&q=80',
  },
]

const heroMetrics = [
  { value: '8', label: '核心业务模块' },
  { value: '24h', label: '持续态势监测' },
  { value: 'AI', label: '路况辅助研判' },
]

const overviewCards: IconCard[] = [
  { title: '统一工作台', description: '首页、控制台、地图、统计、告警和 AI 助手形成完整运营入口。', icon: Monitor },
  { title: '边缘侧推理', description: 'FastAPI 边缘节点支持视频模拟、状态上报和现场 AI 推理扩展。', icon: Cpu },
  { title: '中心 API 服务', description: 'Spring Boot 服务承载权限、数据、告警、站点设置和开放 API。', icon: Database },
  { title: '可视化治理', description: 'Vue 3 仪表盘面向管理人员呈现实时态势、趋势和处置线索。', icon: Layers },
]

const features: FeatureCard[] = [
  {
    title: '实时视频监测',
    desc: '随时查看道路画面，快速定位异常路段与重点区域。',
    icon: Video,
    tone: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  },
  {
    title: '实时状态推送',
    desc: '在地图上直观看到道路态势，掌握拥堵与事件分布。',
    icon: MapPin,
    tone: 'bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300',
  },
  {
    title: '异常事件告警',
    desc: '自动汇总异常并跟踪处理进度，减少漏报与延误。',
    icon: BellRing,
    tone: 'bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300',
  },
  {
    title: '历史数据统计',
    desc: '按时间维度查看趋势变化，支持复盘与成效评估。',
    icon: BarChart,
    tone: 'bg-violet-100 text-violet-700 dark:bg-violet-500/15 dark:text-violet-300',
  },
]

const promoCards: PromoCard[] = [
  {
    title: '实时态势大屏',
    desc: '趋势、告警与态势一屏掌握，适合路演展示。',
    image:
      'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20urban%20traffic%20control%20center%20dashboard%20on%20a%20large%20screen%2C%20busy%20city%20road%20traffic%2C%20cinematic%20lighting%2C%20ultra%20realistic%2C%20high%20detail%2C%20photography%2C%2035mm%2C%20sharp%20focus&image_size=landscape_16_9',
  },
  {
    title: '地图态势展示',
    desc: '道路状态与事件分布清晰可见，便于讲解。',
    image:
      'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20city%20map%20traffic%20visualization%20on%20a%20modern%20monitor%2C%20traffic%20heatmap%2C%20pins%20and%20routes%2C%20office%20environment%2C%20ultra%20realistic%2C%20high%20detail%2C%20photography&image_size=landscape_16_9',
  },
  {
    title: '告警闭环演示',
    desc: '异常发现到处理跟进流程更直观、更可信。',
    image:
      'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=realistic%20smart%20city%20traffic%20incident%20alert%20interface%20on%20laptop%2C%20notification%20panel%2C%20busy%20road%20background%2C%20ultra%20realistic%2C%20high%20detail%2C%20photography&image_size=landscape_16_9',
  },
]

const workflow: WorkflowStep[] = [
  { index: '1', title: '视频与节点接入', description: '道路摄像机和边缘节点持续产生车流、速度、状态和事件数据。' },
  { index: '2', title: '边缘推理过滤', description: '边缘端先完成识别、聚合和异常初判，减少原始视频回传压力。' },
  { index: '3', title: '中心汇聚治理', description: '后端统一接收遥测、落库、触发告警，并向地图和统计模块提供数据。' },
  { index: '4', title: 'AI 辅助决策', description: 'AI 助手结合实时上下文与历史趋势，形成可读的研判说明和处置建议。' },
]

const activeSlide = computed(() => slides[activeSlideIndex.value])

function goToSlide(index: number) {
  activeSlideIndex.value = index
  restartCarousel()
}

function nextSlide() {
  activeSlideIndex.value = (activeSlideIndex.value + 1) % slides.length
  restartCarousel()
}

function previousSlide() {
  activeSlideIndex.value = (activeSlideIndex.value + slides.length - 1) % slides.length
  restartCarousel()
}

function startCarousel() {
  carouselTimer = window.setInterval(() => {
    activeSlideIndex.value = (activeSlideIndex.value + 1) % slides.length
  }, 6000)
}

function stopCarousel() {
  if (carouselTimer) {
    window.clearInterval(carouselTimer)
    carouselTimer = undefined
  }
}

function restartCarousel() {
  stopCarousel()
  startCarousel()
}

onMounted(async () => {
  startCarousel()

  try {
    const res = await fetch(endpoints.siteSettings)
    if (!res.ok) return
    const settings = normalizeSiteSettings(await res.json())
    siteName.value = settings.site_name || '智能交通监控系统'
    footerText.value = settings.footer_text || ''
    announcement.value = settings.announcement || ''
  } catch {
    // 首页仍可用，站点设置接口不可用时使用默认文案。
  }
})

onUnmounted(() => {
  stopCarousel()
})
</script>
