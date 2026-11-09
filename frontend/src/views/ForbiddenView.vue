<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { homeFor } from '../router'
const auth = useAuthStore()
</script>
<template><main class="message-page"><span class="brand-mark">O<span>.</span></span><p class="eyebrow">ACCESS DENIED / 403</p><h1>暂时无法访问<span class="heading-dot">.</span></h1><p>当前账号没有访问此页面的权限。</p><RouterLink class="primary-button inline-button" :to="auth.user ? homeFor(auth.user.role) : '/login'">返回我的首页 <span>↗</span></RouterLink></main></template>
