<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ApiError } from '../api/client'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
const router = useRouter()
const error = ref('')
const busy = ref(false)
async function logout() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await auth.logout(); await router.replace('/login') }
  catch (cause) { error.value = cause instanceof ApiError ? cause.message : '退出失败，请重试。' }
  finally { busy.value = false }
}
</script>
<template>
  <div class="buyer-shell">
    <div class="top-strip"><span>ORDEROPS / 让生活里的每一单，都刚刚好</span><span>WELCOME IN ✳</span></div>
    <header class="buyer-header">
      <RouterLink class="wordmark" to="/"><span class="brand-mark">O<span>.</span></span> ORDEROPS</RouterLink>
      <nav class="buyer-nav" aria-label="买家导航"><RouterLink to="/" class="active-link">商城首页</RouterLink></nav>
      <div class="header-actions"><span class="user-label"><span class="avatar">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</span><span>{{ auth.user?.username }}</span></span><button class="text-button" :disabled="busy" @click="logout">{{ busy ? '退出中…' : '退出' }} ↗</button></div>
    </header>
    <p v-if="error" class="shell-error" role="alert">{{ error }}</p>
    <main><RouterView /></main>
    <footer class="buyer-footer"><span class="footer-brand">ORDEROPS<span>✳</span></span><span>为每一天，挑选一点好。</span><span>© 2026 ORDEROPS</span></footer>
  </div>
</template>
