<script setup lang="ts">
import { cn } from '@/lib/utils'
import { alertVariants, type AlertVariants } from '.'
interface Props { variant?: AlertVariants['variant']; class?: string }
const props = defineProps<Props>()

</script>
<template><div :class="cn(alertVariants({ variant }), props.class)" role="alert"><slot /></div></template>
