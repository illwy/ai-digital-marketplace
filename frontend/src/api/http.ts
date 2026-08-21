import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorBody, ApiErrorResponse } from '../types/api'

const ACCESS_KEY = 'marketplace.accessToken'
const REFRESH_KEY = 'marketplace.refreshToken'

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return sessionStorage.getItem(REFRESH_KEY)
}

export function setTokens(accessToken: string, refreshToken: string): void {
  sessionStorage.setItem(ACCESS_KEY, accessToken)
  sessionStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens(): void {
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
}

export function readApiError(error: unknown): ApiErrorBody {
  const axiosError = error as AxiosError<ApiErrorResponse>
  const body = axiosError.response?.data?.error
  if (body?.message) {
    return body
  }
  return { code: 'NETWORK_ERROR', message: '网络异常，请稍后重试' }
}

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshPromise: Promise<boolean> | null = null

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return false
  }
  try {
    const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken })
    const payload = data.data as { accessToken: string; refreshToken: string }
    setTokens(payload.accessToken, payload.refreshToken)
    return true
  } catch {
    clearTokens()
    return false
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const url = config?.url ?? ''
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh')
    if (status !== 401 || !config || config._retry || isAuthCall) {
      return Promise.reject(error)
    }
    config._retry = true
    if (!refreshPromise) {
      refreshPromise = refreshAccessToken().finally(() => {
        refreshPromise = null
      })
    }
    const ok = await refreshPromise
    if (!ok) {
      return Promise.reject(error)
    }
    return http(config)
  },
)
