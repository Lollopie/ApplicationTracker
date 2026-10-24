const API_BASE = '/api'
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

export type FieldErrors = Record<string, string[]>

/** An RFC 9457 problem detail returned by the backend, or a network failure (status 0). */
export class ApiError extends Error {
  readonly status: number
  readonly detail: string
  readonly fieldErrors: FieldErrors

  constructor(status: number, detail: string, fieldErrors: FieldErrors = {}) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.fieldErrors = fieldErrors
  }
}

type UnauthorizedListener = () => void
let onUnauthorized: UnauthorizedListener | null = null

/** Called whenever an authenticated request comes back 401 (session expired or logged out elsewhere). */
export function setUnauthorizedListener(listener: UnauthorizedListener | null) {
  onUnauthorized = listener
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const match = document.cookie.split('; ').find((cookie) => cookie.startsWith(prefix))
  return match ? decodeURIComponent(match.slice(prefix.length)) : null
}

function clearCsrfCookie() {
  document.cookie = `${CSRF_COOKIE}=; Max-Age=0; path=/`
}

function url(path: string): string {
  // Absolute URL so the same code works in the browser and in jsdom-based tests.
  return new URL(API_BASE + path, window.location.origin).toString()
}

/** Asks the backend to issue a CSRF cookie. The login response clears it, so call this again after logging in. */
export async function refreshCsrfToken(): Promise<void> {
  await fetch(url('/csrf'), { credentials: 'same-origin' })
}

async function csrfToken(): Promise<string | null> {
  if (!readCookie(CSRF_COOKIE)) {
    await refreshCsrfToken()
  }
  return readCookie(CSRF_COOKIE)
}

async function toApiError(response: Response): Promise<ApiError> {
  let detail = response.statusText || `Request failed with status ${response.status}`
  let fieldErrors: FieldErrors = {}
  try {
    const problem = await response.json()
    if (typeof problem.detail === 'string') detail = problem.detail
    if (typeof problem.error === 'string') detail = problem.error
    if (problem.errors && typeof problem.errors === 'object') fieldErrors = problem.errors
  } catch {
    // No JSON body, keep the status text.
  }
  return new ApiError(response.status, detail, fieldErrors)
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function send(method: Method, path: string, body: unknown): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') {
    const token = await csrfToken()
    if (token) headers[CSRF_HEADER] = token
  }
  try {
    return await fetch(url(path), {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check your connection and try again.')
  }
}

export async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  let response = await send(method, path, body)

  // A 403 on a state-changing request means the CSRF token was missing or stale: fetch a fresh one and retry once.
  if (response.status === 403 && method !== 'GET') {
    clearCsrfCookie()
    await refreshCsrfToken()
    response = await send(method, path, body)
  }

  if (!response.ok) {
    const error = await toApiError(response)
    if (error.status === 401 && path !== '/login') onUnauthorized?.()
    throw error
  }

  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}
