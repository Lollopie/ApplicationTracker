import { refreshCsrfToken, request } from './client'

export interface Credentials {
  username: string
  password: string
}

export interface Me {
  username: string
}

export function register(credentials: Credentials) {
  return request<void>('POST', '/register', credentials)
}

export async function login(credentials: Credentials) {
  await request<void>('POST', '/login', credentials)
  await refreshCsrfToken()
}

export async function logout() {
  await request<void>('POST', '/logout')
  await refreshCsrfToken()
}

export function fetchMe() {
  return request<Me>('GET', '/me')
}
