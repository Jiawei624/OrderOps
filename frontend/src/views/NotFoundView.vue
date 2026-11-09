<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="panel" aria-labelledby="not-found-title">
    <h1 id="not-found-title">页面不存在</h1>
    <RouterLink to="/">返回商城</RouterLink>
  </section>
</template>
