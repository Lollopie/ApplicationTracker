import type { JobApplication, Status } from '../api/applications'

/** Mirrors JobApplicationStatus.allowedTransitionSet() so the UI only offers valid moves. The backend still enforces them. */
export const allowedTransitions: Record<Status, Status[]> = {
  SAVED: ['APPLIED', 'REJECTED'],
  APPLIED: ['INTERVIEW', 'REJECTED'],
  INTERVIEW: ['INTERVIEW', 'OFFER', 'REJECTED'],
  OFFER: ['REJECTED'],
  REJECTED: [],
}

/** The backend orders the history oldest first, so the current status is the last entry. */
export function currentStatus(application: JobApplication): Status {
  return application.statusHistory[application.statusHistory.length - 1].status
}

export const statusLabels: Record<Status, string> = {
  SAVED: 'Saved',
  APPLIED: 'Applied',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
}

export const statusBadgeClasses: Record<Status, string> = {
  SAVED: 'bg-slate-100 text-slate-700 ring-slate-300',
  APPLIED: 'bg-blue-50 text-blue-700 ring-blue-300',
  INTERVIEW: 'bg-amber-50 text-amber-800 ring-amber-300',
  OFFER: 'bg-emerald-50 text-emerald-700 ring-emerald-300',
  REJECTED: 'bg-rose-50 text-rose-700 ring-rose-300',
}

/** Label for the button that moves an application into the given status. */
export function transitionLabel(from: Status, to: Status): string {
  if (from === 'INTERVIEW' && to === 'INTERVIEW') return 'Another interview round'
  return `Mark as ${statusLabels[to].toLowerCase()}`
}
