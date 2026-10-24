import { describe, expect, it } from 'vitest'
import type { JobApplication } from '../api/applications'
import { allowedTransitions, currentStatus, transitionLabel } from './status'

describe('currentStatus', () => {
  it('is the last entry of the status history', () => {
    const application: JobApplication = {
      id: '1',
      company: 'X',
      position: 'Y',
      statusHistory: [
        { status: 'SAVED', cause: 'MANUAL', createdAt: '2026-01-01T00:00:00Z' },
        { status: 'APPLIED', cause: 'MANUAL', createdAt: '2026-01-02T00:00:00Z' },
        { status: 'INTERVIEW', cause: 'MANUAL', createdAt: '2026-01-03T00:00:00Z' },
      ],
    }
    expect(currentStatus(application)).toBe('INTERVIEW')
  })
})

describe('allowedTransitions', () => {
  it('matches the backend state machine', () => {
    expect(allowedTransitions).toEqual({
      SAVED: ['APPLIED', 'REJECTED'],
      APPLIED: ['INTERVIEW', 'REJECTED'],
      INTERVIEW: ['INTERVIEW', 'OFFER', 'REJECTED'],
      OFFER: ['REJECTED'],
      REJECTED: [],
    })
  })

  it('labels a repeated interview as another round', () => {
    expect(transitionLabel('INTERVIEW', 'INTERVIEW')).toBe('Another interview round')
    expect(transitionLabel('APPLIED', 'INTERVIEW')).toBe('Mark as interview')
  })
})
