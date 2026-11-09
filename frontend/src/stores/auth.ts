import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ApiError, apiRequest, clearCsrf, loginRequest, logoutRequest, type Identity } from '../api/client'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<Identity | null>(null)
  const initialized = ref(false)
  let pending: Promise<void> | null = null

  function clear() { user.value = null; initialized.value = true; clearCsrf() }
  async function restore(force = false) {
    if (initialized.value && !force) return
    if (pending) return pending
    pending = (async () => {
      try {
        user.value = await apiRequest<Identity>('/api/auth/me', {}, { notifyUnauthorized: false })
        initialized.value = true
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) { clear(); return }
        throw error
      }
    })().finally(() => { pending = null })
    return pending
  }
  async function login(username: string, password: string) {
    await loginRequest(username, password)
    initialized.value = false
    await restore(true)
    if (!user.value) throw new ApiError(401, 'UNAUTHENTICATED', '登录状态未能建立，请重试。')
  }
  async function logout() { await logoutRequest(); clear() }
  return { user, initialized, restore, login, logout, clear }
})
