import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'

// client.ts reads BASE_PATH once at import, so each test imports a fresh copy built for /tracker.
async function importClientUnderPrefix() {
  vi.resetModules()
  vi.doMock('../config', () => ({ BASE_PATH: '/tracker', HOME_URL: '/', SERVER_SLEEPS: false }))
  return import('./client')
}

describe('request under a path prefix (VITE_BASE=/tracker/)', () => {
  beforeEach(() => {
    // The page lives under the prefix, so the browser exposes cookies scoped to /tracker.
    window.history.pushState({}, '', '/tracker/applications')
  })

  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/tracker'
    vi.doUnmock('../config')
    window.history.pushState({}, '', '/')
  })

  it('calls the API under the prefix', async () => {
    const { request } = await importClientUnderPrefix()
    const paths: string[] = []
    server.use(
      http.get('*/me', ({ request }) => {
        paths.push(new URL(request.url).pathname)
        return HttpResponse.json({ username: 'user' })
      }),
    )

    await request('GET', '/me')

    expect(paths).toEqual(['/tracker/api/me'])
  })

  it('clears a stale CSRF token at the cookie path the backend uses', async () => {
    const { request } = await importClientUnderPrefix()
    document.cookie = 'XSRF-TOKEN=stale; path=/tracker'
    const sent: (string | null)[] = []
    server.use(
      // The backend is unreachable for a new token, so the retry must go out without the stale one.
      http.get('*/tracker/api/csrf', () => new HttpResponse(null)),
      http.delete('*/tracker/api/applications/1', ({ request }) => {
        sent.push(request.headers.get('X-XSRF-TOKEN'))
        return new HttpResponse(null, { status: 403 })
      }),
    )

    await request('DELETE', '/applications/1').catch(() => {})

    // Clearing at path=/ would leave the /tracker cookie in place, and the retry would send "stale" again.
    expect(sent).toEqual(['stale', null])
    expect(document.cookie).not.toContain('XSRF-TOKEN')
  })
})
