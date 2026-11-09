<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
</script>
<template>
  <section class="ops-welcome"><div><p class="eyebrow">OVERVIEW <span class="eyebrow-line"></span></p><h1>你好，{{ auth.user?.username }}<span class="heading-dot">.</span></h1><p>欢迎回到运营工作台。</p></div><span class="ops-welcome-mark" aria-hidden="true">✳</span></section>
  <section class="ops-panel"><div class="panel-heading"><div><p class="eyebrow">YOUR WORKSPACE</p><h2>一切，从这里开始</h2></div><span>01 / 工作台</span></div><div class="ops-empty"><span class="ops-empty-icon">↗</span><div><h3>工作台已准备就绪</h3><p>商品与订单管理功能将在后续开放。</p></div></div></section>
</template>
