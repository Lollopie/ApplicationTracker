import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import type { JobApplication, Page } from '../api/applications'
import { api, server } from '../test/server'
import { renderPage } from '../test/render'
import { ApplicationsPage } from './ApplicationsPage'

const page: Page<JobApplication> = {
  content: [
    {
      id: 'a1',
      company: 'Acme',
      position: 'Backend Engineer',
      notes: null,
      links: null,
      statusHistory: [
        { status: 'SAVED', cause: 'MANUAL', createdAt: '2026-10-01T10:00:00Z' },
        { status: 'APPLIED', cause: 'MANUAL', createdAt: '2026-10-02T10:00:00Z' },
      ],
    },
    {
      id: 'a2',
      company: 'Globex',
      position: 'Frontend Engineer',
      statusHistory: [{ status: 'OFFER', cause: 'MANUAL', createdAt: '2026-10-03T10:00:00Z' }],
    },
  ],
  page: { size: 20, number: 0, totalElements: 2, totalPages: 1 },
}

describe('ApplicationsPage', () => {
  beforeEach(() => {
    server.use(http.get(api('/me'), () => HttpResponse.json({ username: 'user' })))
  })

  it('renders the applications with their current status', async () => {
    const queries: string[] = []
    server.use(
      http.get(api('/applications'), ({ request }) => {
        queries.push(new URL(request.url).search)
        return HttpResponse.json(page)
      }),
    )
    renderPage(<ApplicationsPage />, { path: '/applications', route: '/applications' })

    const acme = (await screen.findByText('Acme')).closest('tr')!
    expect(within(acme).getByText('Backend Engineer')).toBeInTheDocument()
    expect(within(acme).getByText('Applied')).toBeInTheDocument()
    expect(within(screen.getByText('Globex').closest('tr')!).getByText('Offer')).toBeInTheDocument()
    expect(screen.getByText('Showing 1–2 of 2')).toBeInTheDocument()
    // Only the parameters the backend allows, and no empty filters.
    expect(queries[0]).toBe('?sort=applicationId%2Cdesc&page=0&size=20')
  })

  it('sends the status filter and sort from the URL', async () => {
    const queries: string[] = []
    server.use(
      http.get(api('/applications'), ({ request }) => {
        queries.push(new URL(request.url).search)
        return HttpResponse.json({ ...page, content: [], page: { ...page.page, totalElements: 0, totalPages: 0 } })
      }),
    )
    renderPage(<ApplicationsPage />, {
      path: '/applications',
      route: '/applications?status=OFFER&sort=company,asc&bogus=1',
    })

    expect(await screen.findByText('No applications match these filters.')).toBeInTheDocument()
    expect(queries[0]).toBe('?currentStatus=OFFER&sort=company%2Casc&page=0&size=20')

    await userEvent.click(screen.getByRole('button', { name: /Company/ }))
    await screen.findByText('No applications match these filters.')
    expect(queries.at(-1)).toBe('?currentStatus=OFFER&sort=company%2Cdesc&page=0&size=20')
  })
})
