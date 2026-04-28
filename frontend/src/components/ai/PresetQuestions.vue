<template>
  <div class="text-center py-8">
    <Bot class="h-12 w-12 mx-auto mb-3 opacity-30" />
    <p class="text-muted-foreground text-sm mb-4">开始新的对话，询问关于道路状况的建议</p>
    <div class="flex flex-wrap justify-center gap-2">
      <Button
        v-for="question in presetQuestions"
        :key="question"
        variant="outline"
        size="sm"
        class="text-xs"
        @click="$emit('select', question)"
      >
        {{ question }}
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Bot } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'

defineEmits<{
  select: [question: string]
}>()

const presetQuestions = [
  '当前哪条路最拥堵？',
  '分析各路段交通趋势',
  '有哪些离线节点？',
  '给出分流建议',
]
</script>