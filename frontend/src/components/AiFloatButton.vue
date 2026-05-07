<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-2">
      <!-- 悬浮按钮 -->
      <Button
        v-if="!panelOpen"
        size="icon"
        class="h-14 w-14 rounded-full shadow-lg"
        @click="openPanel"
      >
        <Bot class="h-6 w-6" />
      </Button>

      <!-- 聊天面板 -->
      <div
        v-if="panelOpen"
        class="ai-float-panel border border-border rounded-lg bg-card shadow-xl flex flex-col relative"
        :style="panelStyle"
      >
        <div
          class="ai-float-resize-handle"
          role="separator"
          title="拖拽调整窗口大小"
          aria-label="拖拽调整窗口大小"
          @pointerdown="startResize"
        >
          <Grip class="h-3.5 w-3.5 rotate-45" />
        </div>

        <!-- 头部 -->
        <div class="p-3 pl-7 border-b border-border flex items-center justify-between shrink-0">
          <div class="flex items-center gap-2">
            <Bot class="h-5 w-5 text-primary" />
            <span class="text-sm font-medium">AI 助手</span>
          </div>
          <div class="flex items-center gap-1">
            <Button
              variant="ghost"
              size="icon"
              class="h-7 w-7"
              :disabled="streaming || messages.length === 0 || !currentConversationId"
              title="清空对话"
              aria-label="清空对话"
              @click="clearCurrentConversation"
            >
              <Eraser class="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              class="h-7 w-7"
              title="关闭"
              aria-label="关闭"
              @click="closePanel"
            >
              <X class="h-4 w-4" />
            </Button>
          </div>
        </div>

        <!-- 道路选择 -->
        <div class="px-3 py-2 border-b border-border shrink-0">
          <RoadSelector v-model="roadContext" :disabled="streaming" class="text-xs" />
        </div>

        <!-- 消息列表 -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto p-3 space-y-3">
          <!-- Empty state with preset questions -->
          <PresetQuestions v-if="messages.length === 0 && !streaming" @select="onPresetSelect" />

          <!-- Messages -->
          <template v-else>
            <div v-for="(msg, idx) in messages" :key="msg.id" class="group">
              <ChatMessage
                :message="msg"
                :show-regenerate="idx === messages.length - 1 && msg.role === 'assistant'"
                @regenerate="regenerateLast"
              />
            </div>
          </template>

          <!-- Streaming indicator -->
          <div v-if="streaming" class="flex justify-start">
            <div class="flex items-end gap-2 max-w-[85%]">
              <div class="shrink-0 w-6 h-6 rounded-full flex items-center justify-center bg-muted">
                <Bot class="w-3.5 h-3.5" />
              </div>
              <div class="max-w-[80%] rounded-lg px-3 py-1.5 text-sm bg-muted">
                <MarkdownBody v-if="streamingContent" :content="streamingContent" />
                <span v-else class="animate-pulse">思考中...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="p-3 border-t border-border shrink-0">
          <form @submit.prevent="sendMessage" class="flex items-end gap-2">
            <Textarea
              ref="inputRef"
              v-model="inputText"
              class="flex-1 min-h-[36px] max-h-[80px] resize-none text-sm"
              placeholder="输入问题，按 Enter 发送"
              @keydown.enter.exact.prevent="sendMessage"
              :disabled="streaming"
            />
            <Button
              v-if="streaming"
              type="button"
              size="icon"
              variant="destructive"
              class="h-9 w-9 shrink-0"
              @click="stopGeneration"
            >
              <Square class="h-4 w-4" />
            </Button>
            <Button
              v-else
              type="submit"
              size="icon"
              class="h-9 w-9 shrink-0"
              :disabled="!inputText.trim()"
            >
              <Send class="h-4 w-4" />
            </Button>
          </form>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, X, Send, Square, Eraser, Grip } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { ChatMessage, MarkdownBody, RoadSelector, PresetQuestions } from '@/components/ai'
import { useAiChat } from '@/composables/useAiChat'

const route = useRoute()
const panelOpen = ref(false)
const messagesContainer = ref<HTMLDivElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const panelSize = ref({
  width: 384,
  height: 520,
})

type ResizeState = {
  startX: number
  startY: number
  startWidth: number
  startHeight: number
}

let resizeState: ResizeState | null = null

const {
  conversations,
  messages,
  inputText,
  roadContext,
  streaming,
  streamingContent,
  currentConversationId,
  loadConversations,
  createConversation,
  loadMessages,
  sendMessage,
  stopGeneration,
  regenerateLast,
  clearConversationMessages,
} = useAiChat({ messagesContainer, autoScroll: true })

// 悬浮窗可见页面配置
const visiblePages = ref<string[]>([])

const visible = computed(() => {
  if (route.path === '/ai-assistant') return false
  if (visiblePages.value.length === 0) return true
  return visiblePages.value.some((p) => route.path.startsWith(p))
})

const panelStyle = computed(() => ({
  width: `${panelSize.value.width}px`,
  height: `${panelSize.value.height}px`,
}))

// Track whether float conversation has been initialized
let floatInitialized = false

watch(panelOpen, (open) => {
  if (open) {
    loadSettings()
    loadOrCreateConversation()
  }
})

onMounted(() => {
  loadSettings()
})

onUnmounted(() => {
  stopResize()
})

async function loadSettings() {
  try {
    const res = await fetch('/api/v1/site-settings')
    if (!res.ok) return
    const data = await res.json()
    const raw = data.ai_float_visible_pages || data.aiFloatVisiblePages || ''
    if (raw.trim()) {
      visiblePages.value = raw
        .split(',')
        .map((s: string) => s.trim())
        .filter(Boolean)
    } else {
      visiblePages.value = []
    }
  } catch {}
}

async function loadOrCreateConversation() {
  if (floatInitialized || currentConversationId.value) return
  try {
    await loadConversations()
    const items = conversations.value
    if (items.length > 0) {
      currentConversationId.value = items[0].id
      if (items[0].road_context) {
        roadContext.value = items[0].road_context
      }
      await loadMessages()
      floatInitialized = true
      return
    }
    const created = await createConversation('悬浮窗对话')
    if (created) {
      currentConversationId.value = created.id
      floatInitialized = true
    }
  } catch {
    // Silently fail
  }
}

function openPanel() {
  panelOpen.value = true
}

function closePanel() {
  panelOpen.value = false
}

function startResize(event: PointerEvent) {
  resizeState = {
    startX: event.clientX,
    startY: event.clientY,
    startWidth: panelSize.value.width,
    startHeight: panelSize.value.height,
  }
  window.addEventListener('pointermove', resizePanel)
  window.addEventListener('pointerup', stopResize)
  window.addEventListener('pointercancel', stopResize)
  event.preventDefault()
}

function resizePanel(event: PointerEvent) {
  if (!resizeState) return
  panelSize.value = {
    width: clampPanelWidth(resizeState.startWidth + resizeState.startX - event.clientX),
    height: clampPanelHeight(resizeState.startHeight + resizeState.startY - event.clientY),
  }
}

function stopResize() {
  resizeState = null
  window.removeEventListener('pointermove', resizePanel)
  window.removeEventListener('pointerup', stopResize)
  window.removeEventListener('pointercancel', stopResize)
}

function clampPanelWidth(width: number): number {
  return Math.min(Math.max(width, Math.min(320, window.innerWidth - 48)), window.innerWidth - 48)
}

function clampPanelHeight(height: number): number {
  return Math.min(Math.max(height, Math.min(420, window.innerHeight - 48)), window.innerHeight - 48)
}

async function clearCurrentConversation() {
  if (!currentConversationId.value || streaming.value) return
  await clearConversationMessages(currentConversationId.value)
}

function onPresetSelect(question: string) {
  inputText.value = question
  sendMessage()
}
</script>

<style scoped>
.ai-float-panel {
  min-width: min(320px, calc(100vw - 3rem));
  min-height: min(420px, calc(100vh - 3rem));
  max-width: calc(100vw - 3rem);
  max-height: calc(100vh - 3rem);
  overflow: hidden;
}

.ai-float-resize-handle {
  position: absolute;
  top: 0.25rem;
  left: 0.25rem;
  z-index: 1;
  display: flex;
  height: 1rem;
  width: 1rem;
  cursor: nwse-resize;
  touch-action: none;
  align-items: center;
  justify-content: center;
  color: hsl(var(--muted-foreground) / 0.65);
}
</style>
