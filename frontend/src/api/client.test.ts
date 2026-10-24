import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { api, csrfHandler, problem, server } from '../test/server'
import { ApiError, request } from './client'

describe('request', () => {
  it('fetches a CSRF token first when the cookie is missing and sends it as a header', async () => {
    const seen: (string | null)[] = []
    server.use(
      csrfHandler('fresh-token'),
      http.post(api('/applications'), ({ request }) => {
        seen.push(request.headers.get('X-XSRF-TOKEN'))
        return HttpResponse.json({ id: '1' }, { status: 201 })
      }),
    )

    const result = await request<{ id: string }>('POST', '/applications', { company: 'X' })

    expect(result).toEqual({ id: '1' })
    expect(seen).toEqual(['fresh-token'])
  })

  it('reuses an existing CSRF cookie without calling /csrf', async () => {
    document.cookie = 'XSRF-TOKEN=existing; path=/'
    let csrfCalls = 0
    server.use(
      http.get(api('/csrf'), () => {
        csrfCalls++
        return new HttpResponse(null)
      }),
      http.delete(api('/applications/1'), ({ request }) =>
        request.headers.get('X-XSRF-TOKEN') === 'existing'
          ? new HttpResponse(null, { status: 204 })
          : new HttpResponse(null, { status: 403 }),
      ),
    )

    await expect(request('DELETE', '/applications/1')).resolves.toBeUndefined()
    expect(csrfCalls).toBe(0)
  })

  it('does not send a CSRF header on GET requests', async () => {
    server.use(
      http.get(api('/me'), ({ request }) =>
        HttpResponse.json({ username: 'u', header: request.headers.get('X-XSRF-TOKEN') }),
      ),
    )
    await expect(request('GET', '/me')).resolves.toEqual({ username: 'u', header: null })
  })

  it('refreshes the token and retries once after a 403', async () => {
    document.cookie = 'XSRF-TOKEN=stale; path=/'
    const seen: (string | null)[] = []
    server.use(
      csrfHandler('rotated'),
      http.put(api('/applications/1'), ({ request }) => {
        const token = request.headers.get('X-XSRF-TOKEN')
        seen.push(token)
        return token === 'rotated' ? HttpResponse.json({ id: '1' }) : new HttpResponse(null, { status: 403 })
      }),
    )

    await expect(request('PUT', '/applications/1', {})).resolves.toEqual({ id: '1' })
    expect(seen).toEqual(['stale', 'rotated'])
  })

  it('turns problem details into an ApiError with field errors', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    server.use(
      http.post(api('/applications'), () =>
        problem(400, 'Invalid request content.', { errors: { company: ['Company is required'] } }),
      ),
    )

    const error = await request('POST', '/applications', {}).catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      detail: 'Invalid request content.',
      fieldErrors: { company: ['Company is required'] },
    })
  })

  it('reports the 409 detail from the backend', async () => {
    document.cookie = 'XSRF-TOKEN=t; path=/'
    server.use(
      http.post(api('/applications/1/status'), () => problem(409, 'Cannot change from status REJECTED to status OFFER')),
    )

    await expect(request('POST', '/applications/1/status', { status: 'OFFER' })).rejects.toMatchObject({
      status: 409,
      message: 'Cannot change from status REJECTED to status OFFER',
    })
  })
})
