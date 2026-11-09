<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { homeFor } from '../router'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref(route.query.unavailable ? '暂时无法连接服务，请确认后端已启动后重试。' : '')
const notice = computed(() => route.query.expired ? '登录已过期，请重新登录。' : '')

async function submit() {
  if (loading.value) return
  error.value = ''
  if (!username.value.trim() || !password.value) { error.value = '请输入账号和密码。'; return }
  loading.value = true
  try {
    await auth.login(username.value.trim(), password.value)
    password.value = ''
    const role = auth.user!.role
    const next = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const resolved = next.startsWith('/') && !next.startsWith('//') ? router.resolve(next) : null
    const target = resolved && resolved.name !== 'login' && resolved.name !== 'not-found'
      && (!resolved.meta.role || resolved.meta.role === role) ? resolved.fullPath : homeFor(role)
    await router.replace(target)
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : '登录未完成，请稍后重试。'
  } finally { loading.value = false }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-story" aria-label="OrderOps 简介">
      <div class="story-top"><span class="brand-mark">O<span>.</span></span><span>ORDEROPS <small> / COMMERCE</small></span></div>
      <div class="story-content">
        <p class="eyebrow light">YOUR COMMERCE WORKSPACE <span>— 01</span></p>
        <h1>让每一次<br><em>选购</em>，更从容。</h1>
        <p class="story-copy">从发现好物到管理订单，<br>一切都在这里，有条不紊。</p>
        <div class="story-art" aria-hidden="true"><span class="art-orbit orbit-one"></span><span class="art-orbit orbit-two"></span><span class="art-card"><span class="art-line"></span><span class="art-dot">✳</span><span class="art-label">MADE FOR<br>EVERYDAY</span></span><span class="art-spark">✳</span></div>
      </div>
      <div class="story-footer"><span>ORDEROPS / 2026</span><span>买家与运营工作台 ↗</span></div>
    </section>
    <section class="login-side">
      <div class="side-top"><span>欢迎来到 OrderOps</span><span class="side-index">01 / 登录</span></div>
      <div class="login-form-wrap">
        <div class="mobile-brand"><span class="brand-mark">O<span>.</span></span> ORDEROPS</div>
        <p class="eyebrow">WELCOME BACK <span class="eyebrow-line"></span></p>
        <h2>登录你的账号</h2>
        <p class="form-intro">输入账号信息，继续你的旅程。</p>
        <form class="login-form" @submit.prevent="submit">
          <label for="username">账号</label>
          <input id="username" v-model="username" name="username" autocomplete="username" placeholder="请输入账号" :disabled="loading" required>
          <label for="password">密码</label>
          <input id="password" v-model="password" name="password" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="loading" required>
          <p v-if="error || notice" class="form-message" :class="{ 'is-error': error }" role="alert">{{ error || notice }}</p>
          <button class="primary-button login-button" type="submit" :disabled="loading"><span>{{ loading ? '正在登录…' : '登录' }}</span><span aria-hidden="true">↗</span></button>
        </form>
        <p class="login-help">买家和运营人员使用各自账号登录。<br>需要账号？请联系管理员。</p>
      </div>
      <div class="side-footer"><span>简单一点，做好每一单。</span><span>ORDEROPS © 2026</span></div>
    </section>
  </main>
</template>
