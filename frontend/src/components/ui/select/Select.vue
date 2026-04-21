<script setup lang="ts">
import { cn } from '@/lib/utils'
defineOptions({ inheritAttrs: false })
const props = defineProps<{ modelValue?: string | number; options?: Array<{ label: string; value: string | number }>; placeholder?: string; class?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string | number): void }>()

</script>
<template>
  <div class="relative">
    <select :value="modelValue" @change="emit('update:modelValue', ($event.target as HTMLSelectElement).value)" :class="cn('flex h-10 w-full appearance-none items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50', props.class)" v-bind="$attrs">
      <option v-if="placeholder" value="" disabled selected hidden>{{ placeholder }}</option>
      <option v-for="opt in options" :key="String(opt.value)" :value="opt.value">{{ opt.label }}</option>
    </select>
    <div class="pointer-events-none absolute inset-y-0 right-3 flex items-center"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="h-4 w-4 opacity-50"><polyline points="6 9 12 15 18 9"></polyline></svg></div>
  </div>
</template>
