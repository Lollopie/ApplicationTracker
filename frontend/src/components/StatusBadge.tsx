import type { Status } from '../api/applications'
import { statusBadgeClasses, statusLabels } from '../domain/status'

export function StatusBadge({ status }: { status: Status }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${statusBadgeClasses[status]}`}
    >
      {statusLabels[status]}
    </span>
  )
}
