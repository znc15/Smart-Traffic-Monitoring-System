import { ref, nextTick, type Ref } from 'vue'
import { toast } from 'vue-sonner'
import { authFetch, endpoints } from '../lib/api'

export type Conversation = {
  id: number
  title: string
  road_context: string | null
  created_at: string
  updated_at: string
}

export type ToolCallInfo = {
  name: string
  arguments: string
}

export type Message = {
  id: number
  role: 'user' | 'assistant'
  content: string
  created_at: string
  toolCalls?: ToolCallInfo[]
}

export type UseAiChatOptions = {
  messagesContainer?: Ref<HTMLDivElement | null>
  autoScroll?: boolean
}

export type UseAiChatReturn = {
  conversations: Ref<Conversation[]>
  currentConversationId: Ref<number | null>
  messages: Ref<Message[]>
  inputText: Ref<string>
  roadContext: Ref<string>
  streaming: Ref<boolean>
  streamingContent: Ref<string>
  streamingToolCalls: Ref<ToolCallInfo[]>
  aiAvailable: Ref<boolean>
  abortController: Ref<AbortController | null>
  
  loadConversations: () => Promise<void>
  createConversation: (title?: string, roadContext?: string | null) => Promise<Conversation | null>
  selectConversation: (conv: Conversation) => Promise<void>
  deleteConversation: (id: number) => Promise<void>
  clearConversationMessages: (id: number) => Promise<void>
  updateConversationTitle: (id: number, title: string) => Promise<void>
  loadMessages: () => Promise<void>
  sendMessage: () => Promise<void>
  stopGeneration: () => void
  regenerateLast: () => Promise<void>
  checkStatus: () => Promise<void>
  scrollToBottom: () => Promise<void>
}

export function useAiChat(options: UseAiChatOptions = {}): UseAiChatReturn {
  const { messagesContainer, autoScroll = true } = options

  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<number | null>(null)
  const messages = ref<Message[]>([])
  const inputText = ref('')
  const roadContext = ref('')
  const streaming = ref(false)
  const streamingContent = ref('')
  const streamingToolCalls = ref<ToolCallInfo[]>([])
  const aiAvailable = ref(false)
  const abortController = ref<AbortController | null>(null)

  async function scrollToBottom(): Promise<void> {
    if (!autoScroll) return
    await nextTick()
    if (messagesContainer?.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }

  async function checkStatus(): Promise<void> {
    try {
      const res = await authFetch(endpoints.aiStatus)
      if (res.ok) {
        const data = await res.json()
        aiAvailable.value = data.configured === true
      }
    } catch {
      aiAvailable.value = false
    }
  }

  async function loadConversations(): Promise<void> {
    try {
      const res = await authFetch(endpoints.aiConversations)
      if (res.ok) {
        const data = await res.json()
        // 支持分页格式 {items, total, page, size} 和旧格式数组
        conversations.value = Array.isArray(data) ? data : (data.items || [])
      }
    } catch {
      toast.error('加载对话列表失败')
    }
  }

  async function createConversation(title = '新对话', roadCtx?: string | null): Promise<Conversation | null> {
    try {
      const res = await authFetch(endpoints.aiConversations, {
        method: 'POST',
        body: JSON.stringify({
          title,
          road_context: roadCtx ?? (roadContext.value || null),
        }),
      })
      if (res.ok) {
        const conv = await res.json()
        conversations.value.unshift(conv)
        return conv
      }
    } catch {
      toast.error('创建对话失败')
    }
    return null
  }

  async function selectConversation(conv: Conversation): Promise<void> {
    currentConversationId.value = conv.id
    if (conv.road_context) {
      roadContext.value = conv.road_context
    }
    await loadMessages()
  }

  async function deleteConversation(id: number): Promise<void> {
    try {
      const res = await authFetch(`${endpoints.aiConversations}/${id}`, { method: 'DELETE' })
      if (res.ok) {
        conversations.value = conversations.value.filter(c => c.id !== id)
        if (currentConversationId.value === id) {
          currentConversationId.value = null
          messages.value = []
        }
      }
    } catch {
      toast.error('删除失败')
    }
  }

  async function clearConversationMessages(id: number): Promise<void> {
    try {
      const res = await authFetch(`${endpoints.aiConversations}/${id}/messages`, { method: 'DELETE' })
      if (res.ok) {
        if (currentConversationId.value === id) {
          messages.value = []
        }
        toast.success('已清空对话消息')
      }
    } catch {
      toast.error('清空失败')
    }
  }

  async function updateConversationTitle(id: number, title: string): Promise<void> {
    try {
      const res = await authFetch(`${endpoints.aiConversations}/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ title }),
      })
      if (res.ok) {
        const conv = conversations.value.find(c => c.id === id)
        if (conv) {
          conv.title = title
        }
      }
    } catch {
      toast.error('更新标题失败')
    }
  }

  async function loadMessages(): Promise<void> {
    if (!currentConversationId.value) return
    try {
      const res = await authFetch(`${endpoints.aiConversations}/${currentConversationId.value}/messages`)
      if (res.ok) {
        messages.value = await res.json()
        await scrollToBottom()
      }
    } catch {
      toast.error('加载消息失败')
    }
  }

  function stopGeneration(): void {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    streaming.value = false
    streamingContent.value = ''
    streamingToolCalls.value = []
  }

  async function sendMessage(): Promise<void> {
    if (!inputText.value.trim() || streaming.value || !currentConversationId.value) return

    const text = inputText.value.trim()
    inputText.value = ''
    streaming.value = true
    streamingContent.value = ''

    const tempUserMsg: Message = {
      id: Date.now(),
      role: 'user',
      content: text,
      created_at: new Date().toISOString(),
    }
    messages.value.push(tempUserMsg)
    await scrollToBottom()

    abortController.value = new AbortController()

    try {
      const res = await authFetch(
        `${endpoints.aiConversations}/${currentConversationId.value}/chat`,
        {
          method: 'POST',
          body: JSON.stringify({ content: text }),
          signal: abortController.value.signal,
        },
        120_000,
      )

      if (!res.ok) {
        const err = await res.json().catch(() => null)
        toast.error(err?.detail || '请求失败')
        streaming.value = false
        return
      }

      const reader = res.body?.getReader()
      if (!reader) {
        toast.error('无法读取流式响应')
        streaming.value = false
        return
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const eventBlocks = buffer.split('\n\n')
        buffer = eventBlocks.pop() || ''

        for (const block of eventBlocks) {
          let eventName = ''
          let eventData = ''
          for (const line of block.split('\n')) {
            if (line.startsWith('event:')) eventName = line.slice(6).trim()
            else if (line.startsWith('data:')) eventData = line.slice(5).trim()
          }

          if (eventName === 'done') break
          if (eventName === 'error') {
            toast.error(eventData || 'AI 服务异常')
            continue
          }
          if (eventName === 'chunk' && eventData) {
            streamingContent.value += eventData
            await scrollToBottom()
          }
          if (eventName === 'tool_call' && eventData) {
            try {
              const tc = JSON.parse(eventData)
              streamingToolCalls.value = [...streamingToolCalls.value, { name: tc.name, arguments: tc.arguments }]
              await scrollToBottom()
            } catch { /* ignore parse errors */ }
          }
          if (eventName === 'tool_result' && eventData) {
            await scrollToBottom()
          }
        }
      }

      await loadMessages()
    } catch (e: any) {
      if (e.name === 'AbortError') {
        // User stopped generation
      } else {
        toast.error('发送失败: ' + (e.message || '未知错误'))
      }
    } finally {
      streaming.value = false
      streamingContent.value = ''
      streamingToolCalls.value = []
      abortController.value = null
    }
  }

  async function regenerateLast(): Promise<void> {
    if (messages.value.length < 2 || streaming.value || !currentConversationId.value) return

    // Find the last user message
    let lastUserMsgIndex = -1
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') { lastUserMsgIndex = i; break }
    }
    if (lastUserMsgIndex === -1) return

    const lastUserMsg = messages.value[lastUserMsgIndex]
    
    // Remove all messages after the last user message (including the assistant response)
    messages.value = messages.value.slice(0, lastUserMsgIndex)

    // Re-send the last user message
    inputText.value = lastUserMsg.content
    await sendMessage()
  }

  return {
    conversations,
    currentConversationId,
    messages,
    inputText,
    roadContext,
    streaming,
    streamingContent,
    streamingToolCalls,
    aiAvailable,
    abortController,
    
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
    scrollToBottom,
  }
}