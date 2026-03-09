<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <RouterView v-if="isNoLayoutPage" />
        <AppLayout v-else>
          <RouterView />
        </AppLayout>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { themeOverrides } from './theme'
import AppLayout from './components/AppLayout.vue'

const route = useRoute()
const isNoLayoutPage = computed(() => route.path === '/login' || route.meta.noLayout === true)
</script>
