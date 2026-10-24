import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import {
  listApplications,
  PAGE_SIZES,
  SORT_FIELDS,
  STATUSES,
  type ListParams,
  type SortDirection,
  type SortField,
  type Status,
} from '../api/applications'
import { ErrorMessage } from '../components/ErrorMessage'
import { Pagination } from '../components/Pagination'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { buttonClasses, cardClasses, inputClasses } from '../components/ui'
import { currentStatus, statusLabels } from '../domain/status'
import { queryKeys } from '../queryClient'

const DEFAULT_SIZE = 20
const DEFAULT_SORT = { field: 'applicationId', direction: 'desc' } as const

/** Reads list parameters from the URL, dropping anything the backend would reject with 400. */
function parseParams(search: URLSearchParams): Required<Pick<ListParams, 'sort' | 'page' | 'size'>> & ListParams {
  const status = search.get('status')
  const [sortField, sortDirection] = (search.get('sort') ?? '').split(',')
  const page = Number(search.get('page') ?? '1')
  const size = Number(search.get('size') ?? DEFAULT_SIZE)
  return {
    currentStatus: STATUSES.includes(status as Status) ? (status as Status) : undefined,
    company: search.get('company') ?? undefined,
    position: search.get('position') ?? undefined,
    sort: SORT_FIELDS.includes(sortField as SortField)
      ? { field: sortField as SortField, direction: sortDirection === 'asc' ? 'asc' : 'desc' }
      : DEFAULT_SORT,
    // The URL is one-based for humans, the backend is zero-based.
    page: Number.isInteger(page) && page > 0 ? page - 1 : 0,
    size: PAGE_SIZES.includes(size) ? size : DEFAULT_SIZE,
  }
}

function useDebouncedSearchParam(name: string, delay = 300) {
  const [searchParams, setSearchParams] = useSearchParams()
  const urlValue = searchParams.get(name) ?? ''
  const [value, setValue] = useState(urlValue)

  useEffect(() => {
    if (value === urlValue) return
    const timeout = setTimeout(() => {
      setSearchParams(
        (previous) => {
          const next = new URLSearchParams(previous)
          if (value) next.set(name, value)
          else next.delete(name)
          next.delete('page')
          return next
        },
        { replace: true },
      )
    }, delay)
    return () => clearTimeout(timeout)
    // urlValue is deliberately left out: typing should not be overwritten while the URL catches up.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, name, delay, setSearchParams])

  return [value, setValue] as const
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

export function ApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const params = parseParams(searchParams)
  const [company, setCompany] = useDebouncedSearchParam('company')
  const [position, setPosition] = useDebouncedSearchParam('position')

  const { data, error, isPending, isFetching } = useQuery({
    queryKey: queryKeys.applicationList(params),
    queryFn: () => listApplications(params),
    placeholderData: keepPreviousData,
  })

  function updateParams(changes: Record<string, string | null>) {
    setSearchParams((previous) => {
      const next = new URLSearchParams(previous)
      for (const [key, value] of Object.entries(changes)) {
        if (value === null) next.delete(key)
        else next.set(key, value)
      }
      return next
    })
  }

  function toggleSort(field: SortField) {
    const direction: SortDirection =
      params.sort.field === field ? (params.sort.direction === 'asc' ? 'desc' : 'asc') : field === 'applicationId' ? 'desc' : 'asc'
    updateParams({ sort: `${field},${direction}`, page: null })
  }

  const hasFilters = Boolean(params.currentStatus || params.company || params.position)

  function sortHeader(field: SortField, label: string) {
    const active = params.sort.field === field
    const ariaSort = active ? (params.sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'
    return (
      <th scope="col" aria-sort={ariaSort} className="px-4 py-3 text-left">
        <button
          type="button"
          onClick={() => toggleSort(field)}
          className="inline-flex items-center gap-1 text-xs font-semibold tracking-wide text-slate-600 uppercase hover:text-slate-900"
        >
          {label}
          <span aria-hidden className={active ? 'text-indigo-600' : 'text-slate-300'}>
            {active && params.sort.direction === 'asc' ? '▲' : '▼'}
          </span>
        </button>
      </th>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Applications</h1>
          <p className="text-sm text-slate-600">Track every application from saved to offer.</p>
        </div>
        <Link to="/applications/new" className={buttonClasses.primary}>
          + New application
        </Link>
      </div>

      <div className={`${cardClasses} grid gap-3 p-4 sm:grid-cols-[1fr_1fr_12rem_auto]`}>
        <input
          aria-label="Filter by company"
          placeholder="Company"
          className={inputClasses}
          value={company}
          onChange={(event) => setCompany(event.target.value)}
        />
        <input
          aria-label="Filter by position"
          placeholder="Position"
          className={inputClasses}
          value={position}
          onChange={(event) => setPosition(event.target.value)}
        />
        <select
          aria-label="Filter by status"
          className={inputClasses}
          value={params.currentStatus ?? ''}
          onChange={(event) => updateParams({ status: event.target.value || null, page: null })}
        >
          <option value="">All statuses</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {statusLabels[status]}
            </option>
          ))}
        </select>
        <button
          type="button"
          className={buttonClasses.secondary}
          disabled={!hasFilters}
          onClick={() => {
            setCompany('')
            setPosition('')
            updateParams({ status: null, company: null, position: null, page: null })
          }}
        >
          Clear
        </button>
      </div>

      {error && <ErrorMessage>{error.message}</ErrorMessage>}

      {isPending ? (
        <Spinner label="Loading applications" />
      ) : data ? (
        <>
          <div className={`${cardClasses} overflow-x-auto ${isFetching ? 'opacity-70' : ''}`}>
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50">
                <tr>
                  {sortHeader('company', 'Company')}
                  {sortHeader('position', 'Position')}
                  {sortHeader('currentStatus', 'Status')}
                  {sortHeader('applicationId', 'Added')}
                  <th scope="col" className="px-4 py-3 text-left text-xs font-semibold tracking-wide text-slate-600 uppercase">
                    Last update
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.map((application) => {
                  const history = application.statusHistory
                  return (
                    <tr
                      key={application.id}
                      className="cursor-pointer hover:bg-slate-50"
                      onClick={() => navigate(`/applications/${application.id}`)}
                    >
                      <td className="px-4 py-3 font-medium text-slate-900">
                        <Link
                          to={`/applications/${application.id}`}
                          className="hover:text-indigo-600"
                          // The row navigates too; stop the click here so history gets one entry, not two.
                          onClick={(event) => event.stopPropagation()}
                        >
                          {application.company}
                        </Link>
                      </td>
                      <td className="px-4 py-3 text-slate-700">{application.position}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={currentStatus(application)} />
                      </td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(history[0].createdAt)}</td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(history[history.length - 1].createdAt)}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            {data.content.length === 0 && (
              <div className="px-4 py-12 text-center">
                <p className="font-medium text-slate-900">
                  {hasFilters ? 'No applications match these filters.' : 'No applications yet.'}
                </p>
                {!hasFilters && (
                  <p className="mt-1 text-sm text-slate-600">
                    <Link to="/applications/new" className="text-indigo-600 hover:text-indigo-500">
                      Add your first application
                    </Link>{' '}
                    to start tracking.
                  </p>
                )}
              </div>
            )}
          </div>
          <Pagination
            page={data.page.number}
            totalPages={data.page.totalPages}
            totalElements={data.page.totalElements}
            size={params.size}
            onPageChange={(page) => updateParams({ page: page === 0 ? null : String(page + 1) })}
            onSizeChange={(size) => updateParams({ size: size === DEFAULT_SIZE ? null : String(size), page: null })}
          />
        </>
      ) : null}
    </div>
  )
}
