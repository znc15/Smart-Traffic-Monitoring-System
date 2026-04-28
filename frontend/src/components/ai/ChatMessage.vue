<template>
  <div class="flex" :class="message.role === 'user' ? 'justify-end' : 'justify-start'">
    <div class="flex items-end gap-2 max-w-[85%]" :class="message.role === 'user' ? 'flex-row-reverse' : 'flex-row'">
      <!-- Avatar -->
      <div
        class="shrink-0 w-7 h-7 rounded-full flex items-center justify-center text-xs font-medium"
        :class="message.role === 'user' ? 'bg-primary text-primary-foreground' : 'bg-muted'"
      >
        <User v-if="message.role === 'user'" class="w-4 h-4" />
        <Bot v-else class="w-4 h-4" />
      </div>

      <!-- Message bubble -->
      <div class="flex flex-col gap-1">
        <div
          class="rounded-lg px-4 py-2 text-sm"
          :class="message.role === 'user' ? 'bg-primary text-primary-foreground' : 'bg-muted'"
        >
          <div v-if="message.role === 'user'" class="whitespace-pre-wrap">{{ message.content }}</div>
          <MarkdownBody v-else :content="message.content" />
        </div>

        <!-- Footer: timestamp and actions -->
        <div class="flex items-center gap-2 px-1" :class="message.role === 'user' ? 'justify-end' : 'justify-start'">
          <span class="text-xs text-muted-foreground">{{ formatTime(message.created_at) }}</span>
          
          <!-- Copy button -->
          <Button
            variant="ghost"
            size="icon"
            class="h-5 w-5 opacity-0 group-hover:opacity-100 transition-opacity"
            @click="copyContent"
          >
            <Copy class="w-3 h-3" />
          </Button>

          <!-- Regenerate button (only for assistant messages) -->
          <Button
            v-if="message.role === 'assistant' && showRegenerate"
            variant="ghost"
            size="icon"
            class="h-5 w-5 opacity-0 group-hover:opacity-100 transition-opacity"
            @click="$emit('regenerate')"
          >
            <RefreshCw class="w-3 h-3" />
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bot, User, Copy, RefreshCw } from 'lucide-vue-next'
import { toast } from 'vue-sonner'
import MarkdownBody from './MarkdownBody.vue'
import { Button } from '@/components/ui/button'
import type { Message } from '../../composables/useAiChat'

const props = defineProps<{
  message: Message
  showRegenerate?: boolean
}>()

defineEmits<{
  regenerate: []
}>()

function formatTime(isoString: string): string {
  try {
    const date = new Date(isoString)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

async function copyContent(): Promise<void> {
  try {
    await navigator.clipboard.writeText(props.message.content)
    toast.success('已复制')
  } catch {
    toast.error('复制失败')
  }
}
</script>