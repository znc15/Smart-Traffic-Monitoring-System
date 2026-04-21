<script setup lang="ts">
import { cn } from '@/lib/utils'
import { DialogTitle, type DialogTitleProps } from 'radix-vue'
const props = defineProps<DialogTitleProps & { class?: string }>()

</script>
<template><DialogTitle v-bind="props" :class="cn('text-lg font-semibold leading-none tracking-tight', props.class)"><slot /></DialogTitle></template>
