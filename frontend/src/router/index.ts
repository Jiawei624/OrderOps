import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { Role } from '../api/client'

declare module 'vue-router' {
  interface RouteMeta { role?: Role; requiresAuth?: boolean }
}
export function homeFor(role: Role) { return role === 'OPERATOR' ? '/operator' : '/' }

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/', component: () => import('../layouts/BuyerLayout.vue'), meta: { requiresAuth: true, role: 'BUYER' }, children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
    ] },
    { path: '/operator', component: () => import('../layouts/OperatorLayout.vue'), meta: { requiresAuth: true, role: 'OPERATOR' }, children: [
      { path: '', name: 'operator', component: () => import('../views/OperatorView.vue') },
    ] },
    { path: '/forbidden', name: 'forbidden', component: () => import('../views/ForbiddenView.vue'), meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue') },
  ],
})

router.beforeEach(async to => {
  const auth = useAuthStore()
  if (!to.meta.requiresAuth && to.name !== 'login') return true
  try { await auth.restore() } catch {
    if (to.name === 'login') return true
    return { name: 'login', query: { unavailable: '1', redirect: to.fullPath } }
  }
  if (to.meta.requiresAuth && !auth.user) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.role && auth.user?.role !== to.meta.role) return { name: 'forbidden' }
  if (to.name === 'login' && auth.user) return homeFor(auth.user.role)
  return true
})
