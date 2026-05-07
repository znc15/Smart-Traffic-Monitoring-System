<template>
  <div class="min-h-screen bg-background text-foreground antialiased selection:bg-primary selection:text-primary-foreground">
    <RouterView v-if="isNoLayoutPage" />
    <AppLayout v-else>
      <RouterView />
    </AppLayout>
    <Toaster />
  </div>
</template>

<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import { useDark } from '@vueuse/core'
import { Toaster } from '@/components/ui/sonner'
import AppLayout from './components/AppLayout.vue'

useDark({
  valueDark: 'dark',
  valueLight: 'light',
  initialValue: 'dark',
})

const route = useRoute()
const isNoLayoutPage = computed(() => route.path === '/login' || route.meta.noLayout === true)
</script>
