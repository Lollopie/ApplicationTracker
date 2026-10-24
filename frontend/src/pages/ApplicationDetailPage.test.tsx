import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import type { JobApplication, Status } from '../api/applications'
import { jobApplication, loggedIn } from '../test/fixtures'
import { renderPage } from '../test/render'
import { api, csrfHandler, problem, server } from '../test/server'
import { ApplicationDetailPage } from './ApplicationDetailPage'

function renderDetail() {
  renderPage(<ApplicationDetailPage />, { path: '/applications/:id', route: '/applications/a1' })
}

/** Serves a1 and applies status changes to it, like the backend. Returns the statuses posted. */
function serveApplication(initial: JobApplication) {
  let current = initial
  const posted: Status[] = []
  server.use(
    http.get(api('/applications/a1'), () => HttpResponse.json(current)),
    http.post(api('/applications/a1/status'), async ({ request }) => {
      const { status } = (await request.json()) as { status: Status }
      posted.push(status)
      current = {
        ...current,
        statusHistory: [...current.statusHistory, { status, cause: 'MANUAL', createdAt: '2026-10-20T10:00:00Z' }],
      }
      return HttpResponse.json(current)
    }),
  )
  return posted
}

describe('ApplicationDetailPage', () => {
  beforeEach(() => {
    server.use(loggedIn(), csrfHandler())
  })

  it('shows the application with its notes as links and its history', async () => {
    serveApplication(jobApplication(['SAVED', 'APPLIED']))
    renderDetail()

    expect(await screen.findByRole('heading', { name: 'Acme' })).toBeInTheDocument()
    expect(screen.getByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'https://acme.example/jobs/1' })).toHaveAttribute(
      'href',
      'https://acme.example/jobs/1',
    )
    const history = screen.getByRole('heading', { name: 'History' }).closest('section')!
    // Newest first.
    const statuses = within(history)
      .getAllByRole('listitem')
      .map((item) => item.querySelector('p')?.textContent)
    expect(statuses).toEqual(['Applied', 'Saved'])
  })

  it('offers only the allowed transitions and moves to the next status', async () => {
    const posted = serveApplication(jobApplication(['SAVED', 'APPLIED']))
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Mark as interview' }))

    expect(await screen.findByRole('button', { name: 'Another interview round' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Mark as offer' })).toBeInTheDocument()
    expect(posted).toEqual(['INTERVIEW'])
  })

  it('asks before rejecting, because rejected is final', async () => {
    const posted = serveApplication(jobApplication(['SAVED', 'APPLIED']))
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Mark as rejected' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(posted).toEqual([])

    await userEvent.click(screen.getByRole('button', { name: 'Mark as rejected' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Mark as rejected' }))

    expect(await screen.findByText(/This application is closed/)).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(posted).toEqual(['REJECTED'])
  })

  it('shows the 409 detail and reloads the application after a conflicting status change', async () => {
    let loads = 0
    server.use(
      http.get(api('/applications/a1'), () => {
        loads++
        return HttpResponse.json(jobApplication(['SAVED', 'APPLIED']))
      }),
      http.post(api('/applications/a1/status'), () => problem(409, 'Cannot change from status OFFER to status INTERVIEW')),
    )
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Mark as interview' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot change from status OFFER to status INTERVIEW')
    await expect.poll(() => loads).toBe(2)
  })

  it('deletes after confirmation and goes back to the list', async () => {
    serveApplication(jobApplication(['SAVED']))
    let deleted = false
    server.use(
      http.delete(api('/applications/a1'), () => {
        deleted = true
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderDetail()

    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }))
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }))

    expect(await screen.findByText('Navigated away')).toBeInTheDocument()
    expect(deleted).toBe(true)
  })

  it('explains when the application does not exist', async () => {
    server.use(http.get(api('/applications/a1'), () => problem(404, 'Not found')))
    renderDetail()

    expect(await screen.findByRole('alert')).toHaveTextContent('This application does not exist or was deleted.')
  })
})
