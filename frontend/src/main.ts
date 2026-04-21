import { createApp } from 'vue'
import './assets/index.css'
import App from './App.vue'
import router from './router.ts'

const app = createApp(App)

app.use(router)

router.isReady().then(() => {
  app.mount('#app')
})
