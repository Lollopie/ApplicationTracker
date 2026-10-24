import { request } from './client'

export const STATUSES = ['SAVED', 'APPLIED', 'INTERVIEW', 'OFFER', 'REJECTED'] as const
export type Status = (typeof STATUSES)[number]

export interface StatusChange {
  status: Status
  cause: 'MANUAL'
  detail?: string | null
  createdAt: string
}

export interface JobApplication {
  id: string
  company: string
  position: string
  notes?: string | null
  links?: string | null
  statusHistory: StatusChange[]
}

export interface Page<T> {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

/** The backend caps the page size at 50. */
export const PAGE_SIZES = [10, 20, 50]

export const SORT_FIELDS = ['applicationId', 'company', 'position', 'currentStatus'] as const
export type SortField = (typeof SORT_FIELDS)[number]
export type SortDirection = 'asc' | 'desc'

export interface ListParams {
  currentStatus?: Status
  company?: string
  position?: string
  sort?: { field: SortField; direction: SortDirection }
  page?: number
  size?: number
}

export interface CreateApplication {
  company: string
  position: string
  status: Status
  notes?: string
  links?: string
}

export type UpdateApplication = Omit<CreateApplication, 'status'>

/** Builds the list query string. Empty values are left out because the backend rejects unknown or blank parameters. */
export function toQueryString(params: ListParams): string {
  const query = new URLSearchParams()
  if (params.currentStatus) query.set('currentStatus', params.currentStatus)
  if (params.company?.trim()) query.set('company', params.company.trim())
  if (params.position?.trim()) query.set('position', params.position.trim())
  if (params.sort) query.set('sort', `${params.sort.field},${params.sort.direction}`)
  if (params.page !== undefined) query.set('page', String(params.page))
  if (params.size !== undefined) query.set('size', String(params.size))
  const string = query.toString()
  return string ? `?${string}` : ''
}

export function listApplications(params: ListParams) {
  return request<Page<JobApplication>>('GET', `/applications${toQueryString(params)}`)
}

export function getApplication(id: string) {
  return request<JobApplication>('GET', `/applications/${encodeURIComponent(id)}`)
}

export function createApplication(application: CreateApplication) {
  return request<JobApplication>('POST', '/applications', application)
}

export function updateApplication(id: string, application: UpdateApplication) {
  return request<JobApplication>('PUT', `/applications/${encodeURIComponent(id)}`, application)
}

export function changeStatus(id: string, status: Status) {
  return request<JobApplication>('POST', `/applications/${encodeURIComponent(id)}/status`, { status })
}

export function deleteApplication(id: string) {
  return request<void>('DELETE', `/applications/${encodeURIComponent(id)}`)
}
