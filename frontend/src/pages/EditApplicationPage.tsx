import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'
import { getApplication, updateApplication, type UpdateApplication } from '../api/applications'
import { ApplicationForm } from '../components/ApplicationForm'
import { ErrorMessage } from '../components/ErrorMessage'
import { Spinner } from '../components/Spinner'
import { queryKeys } from '../queryClient'

export function EditApplicationPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: application, error, isPending } = useQuery({
    queryKey: queryKeys.application(id),
    queryFn: () => getApplication(id),
  })

  const mutation = useMutation({
    mutationFn: (values: UpdateApplication) => updateApplication(id, values),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.application(id), updated)
      queryClient.invalidateQueries({ queryKey: queryKeys.applications })
      navigate(`/applications/${id}`, { replace: true })
    },
  })

  if (isPending) return <Spinner label="Loading application" />
  if (error || !application) {
    return (
      <div className="space-y-4">
        <ErrorMessage>{error?.message ?? 'Application not found.'}</ErrorMessage>
        <Link to="/applications" className="text-sm font-medium text-indigo-600 hover:text-indigo-500">
          ← Back to applications
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">
        Edit {application.company} – {application.position}
      </h1>
      <ApplicationForm
        initialValues={{
          company: application.company,
          position: application.position,
          notes: application.notes ?? '',
          links: application.links ?? '',
        }}
        showStatus={false}
        submitLabel="Save changes"
        cancelTo={`/applications/${id}`}
        submitting={mutation.isPending}
        error={mutation.error}
        onSubmit={({ company, position, notes, links }) => mutation.mutate({ company, position, notes, links })}
      />
    </div>
  )
}
