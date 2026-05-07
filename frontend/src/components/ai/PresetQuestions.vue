<template>
  <div class="text-center py-8">
    <Bot class="h-12 w-12 mx-auto mb-3 opacity-30" />
    <p class="text-muted-foreground text-sm mb-4">开始新的对话，询问关于道路状况的建议</p>
    <div class="flex flex-wrap justify-center gap-2">
      <Button
        v-for="question in presetQuestions"
        :key="question.prompt"
        variant="outline"
        size="sm"
        class="max-w-[9rem] whitespace-normal text-xs leading-snug"
        :title="question.prompt"
        @click="$emit('select', question.prompt)"
      >
        {{ question.label }}
      </Button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Bot } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { authFetch, endpoints } from '@/lib/api'
import { buildPresetQuestions } from './presetQuestions'

defineEmits<{
  select: [question: string]
}>()

const roads = ref<string[]>([])
const presetQuestions = computed(() => buildPresetQuestions(roads.value))

onMounted(async () => {
  try {
    const res = await authFetch(endpoints.roads)
    if (!res.ok) return
    const data = await res.json()
    roads.value = data.roadNames || data.road_names || []
  } catch {
    // Keep fallback preset questions available when roads cannot be loaded.
  }
})
</script>
