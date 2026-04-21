<script setup lang="ts">
import { cn } from '@/lib/utils'
import { DialogDescription, type DialogDescriptionProps } from 'radix-vue'
const props = defineProps<DialogDescriptionProps & { class?: string }>()

</script>
<template><DialogDescription v-bind="props" :class="cn('text-sm text-muted-foreground', props.class)"><slot /></DialogDescription></template>
