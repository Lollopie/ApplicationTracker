import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { renderPage } from '../test/render'
import { api, csrfHandler, problem, server } from '../test/server'
import { RegisterPage } from './RegisterPage'

async function fillIn(username: string, password: string, confirm = password) {
  if (username) await userEvent.type(screen.getByLabelText('Username'), username)
  if (password) await userEvent.type(screen.getByLabelText('Password'), password)
  if (confirm) await userEvent.type(screen.getByLabelText('Confirm password'), confirm)
  await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
}

/** Records every register request body and answers with the given response. */
function onRegister(respond: () => Response = () => new HttpResponse(null, { status: 201 })) {
  const bodies: unknown[] = []
  server.use(
    http.post(api('/register'), async ({ request }) => {
      bodies.push(await request.json())
      return respond()
    }),
  )
  return bodies
}

describe('RegisterPage', () => {
  beforeEach(() => {
    server.use(csrfHandler(), http.get(api('/me'), () => problem(401, 'Full authentication is required')))
  })

  it.each([
    ['', 'password123', 'password123', 'Username is required.'],
    ['a'.repeat(33), 'password123', 'password123', 'Username must be between 1 and 32 characters long.'],
    ['bad name', 'password123', 'password123', 'Username can only include alpha-numeric characters and ., _ and -.'],
    ['user', 'short', 'short', 'Password must be between 8 and 64 characters long.'],
    ['user', 'password123', 'password124', 'Passwords do not match.'],
  ])('rejects %j / %j / %j before sending: %s', async (username, password, confirm, message) => {
    const bodies = onRegister()
    renderPage(<RegisterPage />, { path: '/register', route: '/register' })

    await fillIn(username, password, confirm)

    expect(screen.getByText(message)).toBeInTheDocument()
    expect(bodies).toEqual([])
  })

  it('registers and goes to the login page', async () => {
    const bodies = onRegister()
    renderPage(<RegisterPage />, { path: '/register', route: '/register' })

    await fillIn('new.user', 'password123')

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
    expect(bodies).toEqual([{ username: 'new.user', password: 'password123' }])
  })

  it('shows the 409 detail when the username is taken', async () => {
    onRegister(() => problem(409, 'Username already exists.'))
    renderPage(<RegisterPage />, { path: '/register', route: '/register' })

    await fillIn('user', 'password123')

    expect(await screen.findByRole('alert')).toHaveTextContent('Username already exists.')
  })

  it('shows field errors from the backend next to the field', async () => {
    onRegister(() => problem(400, 'Invalid request content.', { errors: { username: ['Username is reserved'] } }))
    renderPage(<RegisterPage />, { path: '/register', route: '/register' })

    await fillIn('admin', 'password123')

    expect(await screen.findByText('Username is reserved')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
