import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../api/http'
import type { TokenPayload, UserView } from '../types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserView | null>(null)
  const ready = ref(false)

  const isAuthenticated = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') === true)
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')

  function applyTokens(payload: TokenPayload): void {
    setTokens(payload.accessToken, payload.refreshToken)
    user.value = payload.user
  }

  async function register(username: string, password: string, nickname?: string): Promise<void> {
    const { data } = await authApi.register({ username, password, nickname })
    applyTokens(data.data)
  }

  async function login(username: string, password: string): Promise<void> {
    const { data } = await authApi.login({ username, password })
    applyTokens(data.data)
  }

  async function logout(): Promise<void> {
    const refreshToken = getRefreshToken()
    try {
      await authApi.logout(refreshToken)
    } catch {
      // still clear local session
    }
    clearTokens()
    user.value = null
  }

  async function hydrate(): Promise<void> {
    if (!getAccessToken() && !getRefreshToken()) {
      ready.value = true
      return
    }
    try {
      const { data } = await authApi.fetchMe()
      user.value = data.data
    } catch {
      clearTokens()
      user.value = null
    } finally {
      ready.value = true
    }
  }

  return {
    user,
    ready,
    isAuthenticated,
    isAdmin,
    displayName,
    register,
    login,
    logout,
    hydrate,
  }
})
