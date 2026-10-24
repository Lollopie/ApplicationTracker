import { http, HttpResponse } from 'msw'
import type { JobApplication, Status } from '../api/applications'
import { api } from './server'

/** An application whose history went through the given statuses, one day apart. */
export function jobApplication(statuses: Status[], overrides: Partial<JobApplication> = {}): JobApplication {
  return {
    id: 'a1',
    company: 'Acme',
    position: 'Backend Engineer',
    notes: 'Referral from Sam',
    links: 'https://acme.example/jobs/1',
    statusHistory: statuses.map((status, index) => ({
      status,
      cause: 'MANUAL',
      createdAt: new Date(Date.UTC(2026, 9, 1 + index, 10)).toISOString(),
    })),
    ...overrides,
  }
}

/** A logged-in session, so pages behind AuthProvider render for "user". */
export function loggedIn() {
  return http.get(api('/me'), () => HttpResponse.json({ username: 'user' }))
}
