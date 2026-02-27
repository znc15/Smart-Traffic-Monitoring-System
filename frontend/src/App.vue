<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <RouterView v-if="isLoginPage" />
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
const isLoginPage = computed(() => route.path === '/login')
</script>
