<template>
  <div class="flex items-center gap-2">
    <Label class="text-sm whitespace-nowrap">道路：</Label>
    <Select
      :model-value="modelValue"
      @update:model-value="$emit('update:modelValue', String($event))"
      :options="roadOptions"
      placeholder="选择道路"
      class="flex-1"
      :disabled="disabled"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { authFetch, endpoints } from '../../lib/api'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'

const props = defineProps<{
  modelValue: string
  disabled?: boolean
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()

const roads = ref<string[]>([])

const roadOptions = computed(() => {
  const opts = roads.value.map((road) => ({ label: road, value: road }))
  return [{ label: '全部道路', value: '' }, ...opts]
})

onMounted(async () => {
  try {
    const res = await authFetch(endpoints.roads)
    if (res.ok) {
      const data = await res.json()
      roads.value = data.roadNames || data.road_names || []
    }
  } catch {
    // Silently fail
  }
})
</script>