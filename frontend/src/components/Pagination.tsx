import { PAGE_SIZES } from '../api/applications'

interface PaginationProps {
  /** Zero-based, like the backend. */
  page: number
  totalPages: number
  totalElements: number
  size: number
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
}

export function Pagination({ page, totalPages, totalElements, size, onPageChange, onSizeChange }: PaginationProps) {
  const first = totalElements === 0 ? 0 : page * size + 1
  const last = Math.min((page + 1) * size, totalElements)
  const buttonClass =
    'rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50'

  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
      <p>
        {totalElements === 0 ? 'No results' : `Showing ${first}–${last} of ${totalElements}`}
      </p>
      <div className="flex items-center gap-3">
        <label className="flex items-center gap-2">
          Per page
          <select
            value={size}
            onChange={(event) => onSizeChange(Number(event.target.value))}
            className="rounded-md border border-slate-300 bg-white px-2 py-1.5"
          >
            {PAGE_SIZES.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className={buttonClass} disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </button>
        <span>
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>
        <button
          type="button"
          className={buttonClass}
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </nav>
  )
}
