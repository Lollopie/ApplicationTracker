import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { changeStatus, deleteApplication, getApplication, type Status } from '../api/applications'
import { ApiError } from '../api/client'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ErrorMessage } from '../components/ErrorMessage'
import { Linkified } from '../components/Linkified'
import { Spinner } from '../components/Spinner'
import { StatusBadge } from '../components/StatusBadge'
import { buttonClasses, cardClasses } from '../components/ui'
import { allowedTransitions, currentStatus, statusLabels, transitionLabel } from '../domain/status'
import { queryKeys } from '../queryClient'

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirm, setConfirm] = useState<'delete' | 'reject' | null>(null)

  const { data: application, error, isPending } = useQuery({
    queryKey: queryKeys.application(id),
    queryFn: () => getApplication(id),
  })

  const statusMutation = useMutation({
    mutationFn: (status: Status) => changeStatus(id, status),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.application(id), updated)
      queryClient.invalidateQueries({ queryKey: queryKeys.applications })
      setConfirm(null)
    },
    onError: (mutationError) => {
      // 409: an illegal transition or a concurrent change, so show the latest state from the server.
      if (mutationError instanceof ApiError && mutationError.status === 409) {
        queryClient.invalidateQueries({ queryKey: queryKeys.application(id) })
      }
      setConfirm(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteApplication(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: queryKeys.application(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.applications })
      navigate('/applications', { replace: true })
    },
    onError: () => setConfirm(null),
  })

  if (isPending) return <Spinner label="Loading application" />
  if (error || !application) {
    const notFound = error instanceof ApiError && (error.status === 404 || error.status === 400)
    return (
      <div className="space-y-4">
        <ErrorMessage>{notFound ? 'This application does not exist or was deleted.' : error?.message}</ErrorMessage>
        <Link to="/applications" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
          ← Back to applications
        </Link>
      </div>
    )
  }

  const status = currentStatus(application)
  const transitions = allowedTransitions[status]
  const mutationError = statusMutation.error ?? deleteMutation.error
  const history = [...application.statusHistory].reverse()

  return (
    <div className="space-y-6">
      <Link to="/applications" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
        ← Back to applications
      </Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold text-slate-900">{application.company}</h1>
            <StatusBadge status={status} />
          </div>
          <p className="mt-1 text-slate-600">{application.position}</p>
        </div>
        <div className="flex gap-3">
          <Link to={`/applications/${id}/edit`} className={buttonClasses.secondary}>
            Edit
          </Link>
          <button type="button" className={buttonClasses.danger} onClick={() => setConfirm('delete')}>
            Delete
          </button>
        </div>
      </div>

      {mutationError && <ErrorMessage>{mutationError.message}</ErrorMessage>}

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <div className="space-y-6">
          <section className={`${cardClasses} p-6`}>
            <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">Next step</h2>
            {transitions.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">This application is closed. No further status changes are possible.</p>
            ) : (
              <div className="mt-3 flex flex-wrap gap-3">
                {transitions.map((next) => (
                  <button
                    key={next}
                    type="button"
                    disabled={statusMutation.isPending}
                    className={next === 'REJECTED' ? buttonClasses.secondary : buttonClasses.primary}
                    onClick={() => (next === 'REJECTED' ? setConfirm('reject') : statusMutation.mutate(next))}
                  >
                    {transitionLabel(status, next)}
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className={`${cardClasses} p-6`}>
            <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">Notes</h2>
            <div className="mt-3 text-sm text-slate-800">
              {application.notes ? <Linkified text={application.notes} /> : <span className="text-slate-400">No notes.</span>}
            </div>
          </section>

          <section className={`${cardClasses} p-6`}>
            <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">Links</h2>
            <div className="mt-3 text-sm text-slate-800">
              {application.links ? <Linkified text={application.links} /> : <span className="text-slate-400">No links.</span>}
            </div>
          </section>
        </div>

        <section className={`${cardClasses} p-6`}>
          <h2 className="text-sm font-semibold tracking-wide text-slate-500 uppercase">History</h2>
          <ol className="mt-4 space-y-4 border-l border-slate-200 pl-4">
            {history.map((change, index) => (
              <li key={`${change.createdAt}-${index}`} className="relative">
                <span
                  aria-hidden
                  className={`absolute top-1.5 -left-[1.3rem] size-2.5 rounded-full ${index === 0 ? 'bg-indigo-600' : 'bg-slate-300'}`}
                />
                <p className="text-sm font-medium text-slate-900">{statusLabels[change.status]}</p>
                <p className="text-xs text-slate-500">
                  <time dateTime={change.createdAt}>{formatDateTime(change.createdAt)}</time>
                </p>
                {change.detail && <p className="mt-1 text-sm text-slate-600">{change.detail}</p>}
              </li>
            ))}
          </ol>
        </section>
      </div>

      <ConfirmDialog
        open={confirm === 'delete'}
        title="Delete application?"
        confirmLabel="Delete"
        busy={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setConfirm(null)}
      >
        This permanently deletes the application at {application.company} and its status history.
      </ConfirmDialog>
      <ConfirmDialog
        open={confirm === 'reject'}
        title="Mark as rejected?"
        confirmLabel="Mark as rejected"
        busy={statusMutation.isPending}
        onConfirm={() => statusMutation.mutate('REJECTED')}
        onCancel={() => setConfirm(null)}
      >
        Rejected is a final status. The application can't be moved to another status afterwards.
      </ConfirmDialog>
    </div>
  )
}
