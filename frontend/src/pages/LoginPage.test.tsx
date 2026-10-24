import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { api, csrfHandler, problem, server } from '../test/server'
import { renderPage } from '../test/render'
import { LoginPage } from './LoginPage'

describe('LoginPage', () => {
  it('shows an error for invalid credentials', async () => {
    server.use(
      csrfHandler(),
      http.get(api('/me'), () => problem(401, 'Full authentication is required')),
      http.post(api('/login'), () => problem(401, 'Bad credentials')),
    )
    renderPage(<LoginPage />, { path: '/login', route: '/login' })

    await userEvent.type(screen.getByLabelText('Username'), 'user')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-password')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password.')
  })

  it('logs in and navigates away on success', async () => {
    let loggedIn = false
    server.use(
      csrfHandler(),
      http.get(api('/me'), () =>
        loggedIn ? HttpResponse.json({ username: 'user' }) : problem(401, 'Full authentication is required'),
      ),
      http.post(api('/login'), async ({ request }) => {
        expect(await request.json()).toEqual({ username: 'user', password: 'password1' })
        loggedIn = true
        return new HttpResponse(null, { status: 200 })
      }),
    )
    renderPage(<LoginPage />, { path: '/login', route: '/login' })

    await userEvent.type(screen.getByLabelText('Username'), 'user')
    await userEvent.type(screen.getByLabelText('Password'), 'password1')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
  })
})
