import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { jobApplication, loggedIn } from '../test/fixtures'
import { renderPage } from '../test/render'
import { api, csrfHandler, problem, server } from '../test/server'
import { EditApplicationPage } from './EditApplicationPage'

function renderEdit() {
  renderPage(<EditApplicationPage />, { path: '/applications/:id/edit', route: '/applications/a1/edit' })
}

describe('EditApplicationPage', () => {
  beforeEach(() => {
    server.use(loggedIn(), csrfHandler())
  })

  it('starts from the saved values and has no status field', async () => {
    server.use(http.get(api('/applications/a1'), () => HttpResponse.json(jobApplication(['SAVED']))))
    renderEdit()

    expect(await screen.findByLabelText(/Company/)).toHaveValue('Acme')
    expect(screen.getByLabelText(/Position/)).toHaveValue('Backend Engineer')
    expect(screen.getByLabelText('Notes')).toHaveValue('Referral from Sam')
    expect(screen.queryByLabelText('Initial status')).not.toBeInTheDocument()
  })

  it('clears emptied fields by leaving them out of the PUT', async () => {
    const bodies: unknown[] = []
    server.use(
      http.get(api('/applications/a1'), () => HttpResponse.json(jobApplication(['SAVED']))),
      http.put(api('/applications/a1'), async ({ request }) => {
        bodies.push(await request.json())
        return HttpResponse.json(jobApplication(['SAVED'], { notes: null }))
      }),
    )
    renderEdit()

    await userEvent.clear(await screen.findByLabelText('Notes'))
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
    expect(bodies).toEqual([{ company: 'Acme', position: 'Backend Engineer', links: 'https://acme.example/jobs/1' }])
  })

  it('shows the error when the application cannot be loaded', async () => {
    server.use(http.get(api('/applications/a1'), () => problem(404, 'Application not found')))
    renderEdit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Application not found')
  })
})
