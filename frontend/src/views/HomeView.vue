<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
</script>
<template>
  <div class="buyer-content">
    <section class="welcome-banner"><div><p class="eyebrow light">A LITTLE SOMETHING FOR YOU <span>— 01</span></p><h1>你好，{{ auth.user?.username }}。<br><em>今天想发现什么？</em></h1><p>好好生活，从每一次用心挑选开始。</p></div><div class="welcome-seal" aria-hidden="true"><span>✳</span><small>GOOD DAYS<br>START HERE</small></div><div class="banner-index">CURATED FOR EVERYDAY / 01</div></section>
    <section class="catalog-section"><div class="section-heading"><div><p class="eyebrow">THE COLLECTION <span class="eyebrow-line"></span></p><h2>发现好物<span class="heading-dot">.</span></h2><p>值得期待的日常，正在这里慢慢上架。</p></div><span class="section-index">01 / 商品</span></div><div class="empty-catalog"><div class="empty-icon">✳</div><h3>好物正在路上</h3><p>目前还没有上架商品，稍后再来看看吧。</p><span>STAY CURIOUS &nbsp; ↗</span></div></section>
  </div>
</template>
