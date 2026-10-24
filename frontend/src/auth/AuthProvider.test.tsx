import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type { ReactElement } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'
import { listApplications } from '../api/applications'
import { queryKeys } from '../queryClient'
import { testConfig } from '../test/config'
import { loggedIn } from '../test/fixtures'
import { api, csrfHandler, problem, server } from '../test/server'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './context'
import { RequireAuth } from './RequireAuth'

function Greeting() {
  return <p>Hello {useAuth().user?.username}</p>
}

function ApplicationCount() {
  const { data } = useQuery({ queryKey: queryKeys.applicationList({}), queryFn: () => listApplications({}) })
  return <p>{data ? `${data.content.length} applications` : 'Loading'}</p>
}

function LogoutButton() {
  const { logout } = useAuth()
  return (
    // Like Layout: logout() rethrows a failed server call after clearing the session locally.
    <button type="button" onClick={() => logout().catch(() => {})}>
      Log out
    </button>
  )
}

function renderProtectedPage(page: ReactElement = <Greeting />, queryClient = new QueryClient()) {
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AuthProvider>
          <Routes>
            <Route element={<RequireAuth />}>
              <Route path="/" element={page} />
            </Route>
            <Route path="/login" element={<p>Login page</p>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

/** Counts the session checks and answers each one with the given response. */
function meHandler(respond: () => Response) {
  const counter = { calls: 0 }
  server.use(
    http.get(api('/me'), () => {
      counter.calls++
      return respond()
    }),
  )
  return counter
}

describe('AuthProvider', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps retrying the session check while the backend wakes up', async () => {
    testConfig.serverSleeps = true
    let calls = 0
    server.use(
      http.get(api('/me'), () => {
        calls++
        // The first answer is the host's placeholder page, then Spring is up.
        return calls === 1 ? HttpResponse.html('<html>Waking up…</html>') : HttpResponse.json({ username: 'user' })
      }),
    )

    renderProtectedPage()

    expect(await screen.findByText(/Waking up the server/)).toBeInTheDocument()
    expect(screen.getByText(/free hosting tier/)).toBeInTheDocument()
    expect(await screen.findByText('Hello user', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(calls).toBe(2)
  })

  it('does not mention waking up when the host does not put the backend to sleep', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const me = meHandler(() => HttpResponse.text('', { status: 502 }))

    renderProtectedPage()

    expect(await screen.findByText(/Reconnecting to the server/)).toBeInTheDocument()
    expect(screen.queryByText(/Waking up|free hosting tier/)).not.toBeInTheDocument()
    // Gives up after a few retries instead of two minutes.
    await vi.advanceTimersByTimeAsync(10_000)
    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(me.calls).toBe(4)
  })

  it('gives up quickly on a failed connection and shows the login page', async () => {
    testConfig.serverSleeps = true
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const me = meHandler(() => HttpResponse.error())

    renderProtectedPage()

    expect(await screen.findByText(/Checking your session/)).toBeInTheDocument()
    await vi.advanceTimersByTimeAsync(10_000)
    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText(/Waking up/)).not.toBeInTheDocument()
    expect(me.calls).toBe(3)
  })

  it('logs the user out when any request comes back 401, e.g. after the session expired', async () => {
    server.use(
      loggedIn(),
      http.get(api('/applications'), () => problem(401, 'Full authentication is required')),
    )

    renderProtectedPage(<ApplicationCount />)

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it("drops the previous user's cached data on logout", async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(queryKeys.applicationList({}), { content: [], page: {} })
    server.use(
      loggedIn(),
      csrfHandler(),
      http.post(api('/logout'), () => new HttpResponse(null, { status: 200 })),
    )
    renderProtectedPage(<LogoutButton />, queryClient)

    await userEvent.click(await screen.findByRole('button', { name: 'Log out' }))

    expect(await screen.findByText('Login page')).toBeInTheDocument()
    expect(queryClient.getQueryData(queryKeys.applicationList({}))).toBeUndefined()
    expect(queryClient.getQueryData(queryKeys.me)).toBeNull()
  })

  it('logs out locally even when the server call fails', async () => {
    server.use(
      loggedIn(),
      csrfHandler(),
      http.post(api('/logout'), () => problem(500, 'Something went wrong')),
    )
    renderProtectedPage(<LogoutButton />)

    await userEvent.click(await screen.findByRole('button', { name: 'Log out' }))

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })
})
