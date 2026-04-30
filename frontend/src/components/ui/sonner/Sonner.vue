<template>
  <VueSonner
    :theme="isDark ? 'dark' : 'light'"
    position="top-right"
    rich-colors
    close-button
  />
</template>

<script setup lang="ts">
import { useDark } from '@vueuse/core'
import { Toaster as VueSonner } from 'vue-sonner'

const isDark = useDark()
</script>
