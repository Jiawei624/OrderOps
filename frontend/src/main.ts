import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { setUnauthorizedHandler } from './api/client'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
setUnauthorizedHandler(() => {
  useAuthStore(pinia).clear()
  if (router.currentRoute.value.name !== 'login') void router.replace({ name: 'login', query: { expired: '1' } })
})
app.use(router).mount('#app')
