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
  <div class="ops-shell">
    <aside class="ops-sidebar"><RouterLink class="ops-brand" to="/operator"><span class="brand-mark">O<span>.</span></span><span>ORDEROPS<small>OPERATIONS</small></span></RouterLink><p class="ops-nav-label">WORKSPACE</p><nav aria-label="运营导航"><RouterLink class="ops-nav-active" to="/operator"><span>▦</span> 工作台 <span class="nav-arrow">↗</span></RouterLink></nav><div class="ops-sidebar-bottom"><span>✳</span><p>让工作更有序。<br><small>YOUR DAILY WORKSPACE</small></p></div></aside>
    <div class="ops-main"><header class="ops-header"><div><span class="breadcrumb">工作空间</span><span class="breadcrumb-divider">/</span><strong>工作台</strong></div><div class="header-actions"><span class="ops-role">运营人员</span><span class="user-label"><span class="avatar">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</span>{{ auth.user?.username }}</span><button class="text-button" :disabled="busy" @click="logout">{{ busy ? '退出中…' : '退出' }} ↗</button></div></header><p v-if="error" class="shell-error" role="alert">{{ error }}</p><main class="ops-content"><RouterView /></main><footer class="ops-footer"><span>ORDEROPS / OPERATIONS</span><span>BUILT FOR BETTER DAYS ✳</span></footer></div>
  </div>
</template>
