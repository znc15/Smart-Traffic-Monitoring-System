<template>
  <div class="flex h-full gap-4">
    <!-- 左侧：对话列表 -->
    <div class="w-64 shrink-0 flex flex-col border border-border rounded-lg bg-card">
      <div class="p-3 border-b border-border flex items-center justify-between">
        <span class="text-sm font-medium">对话历史</span>
        <Button variant="ghost" size="icon" class="h-7 w-7" @click="handleCreateConversation" :disabled="!aiAvailable">
          <Plus class="h-4 w-4" />
        </Button>
      </div>
      <div class="flex-1 overflow-y-auto">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="px-3 py-2 cursor-pointer text-sm hover:bg-accent transition-colors flex items-center justify-between group"
          :class="currentConversationId === conv.id ? 'bg-accent text-accent-foreground' : 'text-muted-foreground'"
          @click="selectConversation(conv)"
        >
          <!-- Renameable title -->
          <input
            v-if="editingId === conv.id"
            ref="renameInput"
            v-model="editingTitle"
            class="truncate flex-1 bg-transparent border-b border-primary outline-none text-sm"
            @blur="finishRename(conv.id)"
            @keydown.enter="finishRename(conv.id)"
            @keydown.escape="cancelRename"
          />
          <span v-else class="truncate flex-1" @dblclick="startRename(conv)">{{ conv.title }}</span>

          <div class="flex items-center gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
            <Button variant="ghost" size="icon" class="h-5 w-5" @click.stop="handleClearMessages(conv.id)" title="清空消息">
              <Eraser class="h-3 w-3" />
            </Button>
            <Button variant="ghost" size="icon" class="h-5 w-5" @click.stop="deleteConversation(conv.id)">
              <Trash2 class="h-3 w-3" />
            </Button>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="p-4 text-sm text-muted-foreground text-center">
          暂无对话
        </div>
      </div>
    </div>

    <!-- 右侧：聊天区域 -->
    <div class="flex-1 flex flex-col border border-border rounded-lg bg-card min-w-0">
      <!-- 顶部栏 -->
      <div class="p-3 border-b border-border flex items-center justify-between gap-4">
        <RoadSelector v-model="roadContext" :disabled="streaming" @update:model-value="onRoadContextChange" />
        <div class="flex items-center gap-2">
          <Badge v-if="!aiAvailable" variant="secondary">AI 未配置</Badge>
          <Badge v-else variant="default">AI 就绪</Badge>
        </div>
      </div>

      <!-- 消息列表 -->
      <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-4">
        <!-- Loading skeleton -->
        <div v-if="messagesLoading" class="space-y-4">
          <div v-for="i in 3" :key="i" class="flex" :class="i % 2 === 0 ? 'justify-end' : 'justify-start'">
            <div class="max-w-[60%] rounded-lg px-4 py-3">
              <Skeleton class="h-4 w-40 mb-2" />
              <Skeleton class="h-4 w-28" />
            </div>
          </div>
        </div>

        <!-- Empty state with preset questions -->
        <PresetQuestions v-else-if="messages.length === 0 && !streaming" @select="onPresetSelect" />

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
            <div class="shrink-0 w-7 h-7 rounded-full flex items-center justify-center bg-muted">
              <Bot class="w-4 h-4" />
            </div>
            <div class="max-w-[80%] rounded-lg px-4 py-2 text-sm bg-muted">
              <!-- Tool call indicators -->
              <div v-if="streamingToolCalls.length > 0 && !streamingContent" class="space-y-1">
                <div v-for="(tc, i) in streamingToolCalls" :key="i" class="flex items-center gap-2 text-xs text-muted-foreground">
                  <Loader2 class="h-3 w-3 animate-spin" />
                  <span>正在调用 {{ toolDisplayName(tc.name) }}...</span>
                </div>
              </div>
              <MarkdownBody v-if="streamingContent" :content="streamingContent" />
              <span v-else-if="streamingToolCalls.length === 0" class="animate-pulse">思考中...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="p-3 border-t border-border">
        <form @submit.prevent="sendMessage" class="flex items-end gap-2">
          <Textarea
            ref="inputRef"
            v-model="inputText"
            class="flex-1 min-h-[40px] max-h-[120px] resize-none text-sm"
            placeholder="输入问题，按 Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="sendMessage"
            :disabled="streaming"
          />
          <Button
            v-if="streaming"
            type="button"
            size="icon"
            variant="destructive"
            @click="stopGeneration"
          >
            <Square class="h-4 w-4" />
          </Button>
          <Button
            v-else
            type="submit"
            size="icon"
            :disabled="!inputText.trim() || !currentConversationId"
          >
            <Send class="h-4 w-4" />
          </Button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { Plus, Trash2, Eraser, Send, Square, Loader2 } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Skeleton } from '@/components/ui/skeleton'
import { ChatMessage, MarkdownBody, RoadSelector, PresetQuestions } from '@/components/ai'
import { useAiChat } from '@/composables/useAiChat'
import type { Conversation } from '@/composables/useAiChat'

const messagesContainer = ref<HTMLDivElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const messagesLoading = ref(false)

const {
  conversations,
  currentConversationId,
  messages,
  inputText,
  roadContext,
  streaming,
  streamingContent,
  streamingToolCalls,
  aiAvailable,
  loadConversations,
  createConversation,
  selectConversation,
  deleteConversation,
  clearConversationMessages,
  updateConversationTitle,
  loadMessages,
  sendMessage,
  stopGeneration,
  regenerateLast,
  checkStatus,
} = useAiChat({ messagesContainer })

// ── Rename ──────────────────────────────────────────────
const editingId = ref<number | null>(null)
const editingTitle = ref('')
const renameInput = ref<HTMLInputElement[] | null>(null)

function startRename(conv: Conversation) {
  editingId.value = conv.id
  editingTitle.value = conv.title
  nextTick(() => {
    renameInput.value?.[0]?.focus()
  })
}

async function finishRename(id: number) {
  if (editingId.value !== id) return
  const title = editingTitle.value.trim()
  editingId.value = null
  if (!title) return
  await updateConversationTitle(id, title)
}

function cancelRename() {
  editingId.value = null
}

// ── Clear messages ──────────────────────────────────────
async function handleClearMessages(id: number) {
  await clearConversationMessages(id)
}

// ── Road context change ─────────────────────────────────
function onRoadContextChange() {
  // Road context is stored per conversation, update is implicit via next sendMessage
}

// ── Tool display name helper ─────────────────────────────
function toolDisplayName(name: string): string {
  const toolNames: Record<string, string> = {
    query_traffic: '交通数据查询',
    list_cameras: '摄像头列表',
    query_history: '历史数据查询',
    reverse_geocode: '位置查询',
  }
  return toolNames[name] || name
}

// ── Preset question click ───────────────────────────────
function onPresetSelect(question: string) {
  inputText.value = question
  sendMessage()
}

// ── Create conversation ─────────────────────────────────
async function handleCreateConversation() {
  const conv = await createConversation('新对话', roadContext.value || null)
  if (conv) {
    await selectConversation(conv)
  }
}

// ── Init ────────────────────────────────────────────────
onMounted(async () => {
  await checkStatus()
  messagesLoading.value = true
  await loadConversations()
  if (conversations.value.length === 0) {
    await handleCreateConversation()
  } else {
    await selectConversation(conversations.value[0])
  }
  messagesLoading.value = false
})

// Sync road context when switching conversations
watch(currentConversationId, () => {
  const conv = conversations.value.find(c => c.id === currentConversationId.value)
  if (conv?.road_context) {
    roadContext.value = conv.road_context
  } else {
    roadContext.value = ''
  }
})
</script>