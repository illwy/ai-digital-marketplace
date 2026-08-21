import { http } from './http'
import type { DataResponse, TokenPayload, UserView } from '../types/api'

export function register(payload: { username: string; password: string; nickname?: string }) {
  return http.post<DataResponse<TokenPayload>>('/auth/register', payload)
}

export function login(payload: { username: string; password: string }) {
  return http.post<DataResponse<TokenPayload>>('/auth/login', payload)
}

export function logout(refreshToken: string | null) {
  return http.post('/auth/logout', { refreshToken })
}

export function fetchMe() {
  return http.get<DataResponse<UserView>>('/me')
}

export function fetchAdminPing() {
  return http.get<DataResponse<{ status: string; username: string }>>('/admin/ping')
}
