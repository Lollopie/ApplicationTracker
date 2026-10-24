import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { createApplication } from '../api/applications'
import { ApplicationForm } from '../components/ApplicationForm'
import { queryKeys } from '../queryClient'

export function NewApplicationPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: createApplication,
    onSuccess: (created) => {
      queryClient.setQueryData(queryKeys.application(created.id), created)
      queryClient.invalidateQueries({ queryKey: queryKeys.applications })
      navigate(`/applications/${created.id}`, { replace: true })
    },
  })

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-semibold text-slate-900">New application</h1>
      <ApplicationForm
        showStatus
        submitLabel="Create application"
        cancelTo="/applications"
        submitting={mutation.isPending}
        error={mutation.error}
        onSubmit={(values) => mutation.mutate(values)}
      />
    </div>
  )
}
