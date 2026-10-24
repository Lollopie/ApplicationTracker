import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'
import { api, server } from '../test/server'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './context'
import { RequireAuth } from './RequireAuth'

function Greeting() {
  return <p>Hello {useAuth().user?.username}</p>
}

describe('AuthProvider', () => {
  it('keeps retrying the session check while the backend wakes up', async () => {
    let calls = 0
    server.use(
      http.get(api('/me'), () => {
        calls++
        // The first answer is the host's placeholder page, then Spring is up.
        return calls === 1 ? HttpResponse.html('<html>Waking up…</html>') : HttpResponse.json({ username: 'user' })
      }),
    )

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <AuthProvider>
            <Routes>
              <Route element={<RequireAuth />}>
                <Route path="/" element={<Greeting />} />
              </Route>
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByText(/Waking up the server/)).toBeInTheDocument()
    expect(await screen.findByText('Hello user', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(calls).toBe(2)
  })
})
