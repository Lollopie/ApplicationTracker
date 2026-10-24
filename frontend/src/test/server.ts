import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

export const server = setupServer()

export const api = (path: string) => `*/api${path}`

/** Mimics GET /csrf: the backend sets a readable XSRF-TOKEN cookie. */
export function csrfHandler(token = 'csrf-token') {
  return http.get(api('/csrf'), () => {
    document.cookie = `XSRF-TOKEN=${token}; path=/`
    return new HttpResponse(null, { status: 200 })
  })
}

export function problem(status: number, detail: string, extra: object = {}) {
  return HttpResponse.json(
    { type: 'about:blank', title: 'Error', status, detail, ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export function clearCookies() {
  for (const cookie of document.cookie.split('; ')) {
    const name = cookie.split('=')[0]
    if (name) document.cookie = `${name}=; Max-Age=0; path=/`
  }
}
