import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { jobApplication, loggedIn } from '../test/fixtures'
import { renderPage } from '../test/render'
import { api, csrfHandler, problem, server } from '../test/server'
import { NewApplicationPage } from './NewApplicationPage'

function renderNew() {
  renderPage(<NewApplicationPage />, { path: '/applications/new', route: '/applications/new' })
}

/** Records every create request body and answers with the given response. */
function onCreate(respond: () => Response = () => HttpResponse.json(jobApplication(['SAVED']), { status: 201 })) {
  const bodies: unknown[] = []
  server.use(
    http.post(api('/applications'), async ({ request }) => {
      bodies.push(await request.json())
      return respond()
    }),
  )
  return bodies
}

describe('NewApplicationPage', () => {
  beforeEach(() => {
    server.use(loggedIn(), csrfHandler())
  })

  it('validates the required fields before sending anything', async () => {
    const bodies = onCreate()
    renderNew()

    await userEvent.type(screen.getByLabelText(/Company/), '   ')
    await userEvent.click(screen.getByRole('button', { name: 'Create application' }))

    expect(screen.getByText('Company is required')).toBeInTheDocument()
    expect(screen.getByText('Position is required')).toBeInTheDocument()
    expect(screen.getByLabelText(/Company/)).toHaveAttribute('aria-invalid', 'true')
    expect(bodies).toEqual([])
  })

  it('rejects notes over the backend limit of 500 characters', async () => {
    const bodies = onCreate()
    renderNew()

    await userEvent.type(screen.getByLabelText(/Company/), 'Acme')
    await userEvent.type(screen.getByLabelText(/Position/), 'Dev')
    await userEvent.click(screen.getByLabelText('Notes'))
    await userEvent.paste('x'.repeat(501))
    await userEvent.click(screen.getByRole('button', { name: 'Create application' }))

    expect(screen.getByText('501/500')).toBeInTheDocument()
    expect(screen.getByText('At most 500 characters')).toBeInTheDocument()
    expect(bodies).toEqual([])
  })

  it('sends trimmed values, leaves out blank fields and opens the new application', async () => {
    const bodies = onCreate()
    renderNew()

    await userEvent.type(screen.getByLabelText(/Company/), '  Acme  ')
    await userEvent.type(screen.getByLabelText(/Position/), 'Dev')
    await userEvent.selectOptions(screen.getByLabelText('Initial status'), 'APPLIED')
    await userEvent.type(screen.getByLabelText('Notes'), '   ')
    await userEvent.click(screen.getByRole('button', { name: 'Create application' }))

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
    expect(bodies).toEqual([{ company: 'Acme', position: 'Dev', status: 'APPLIED' }])
  })

  it('shows field errors from the backend next to the field', async () => {
    onCreate(() => problem(400, 'Invalid request content.', { errors: { position: ['Position is too long'] } }))
    renderNew()

    await userEvent.type(screen.getByLabelText(/Company/), 'Acme')
    await userEvent.type(screen.getByLabelText(/Position/), 'Dev')
    await userEvent.click(screen.getByRole('button', { name: 'Create application' }))

    expect(await screen.findByText('Position is too long')).toBeInTheDocument()
    // Field errors replace the generic message.
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows other errors above the form', async () => {
    onCreate(() => problem(500, 'Something went wrong'))
    renderNew()

    await userEvent.type(screen.getByLabelText(/Company/), 'Acme')
    await userEvent.type(screen.getByLabelText(/Position/), 'Dev')
    await userEvent.click(screen.getByRole('button', { name: 'Create application' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })
})
