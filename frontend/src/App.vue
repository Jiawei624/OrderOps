<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header class="site-header">
    <RouterLink class="brand" to="/">OrderOps</RouterLink>
    <nav aria-label="主导航">
      <RouterLink to="/">商城</RouterLink>
    </nav>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>
