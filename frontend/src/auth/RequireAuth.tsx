import { Navigate, Outlet, useLocation } from 'react-router'
import { Spinner } from '../components/Spinner'
import { useAuth } from './context'

export function RequireAuth() {
  const { user, waking } = useAuth()
  const location = useLocation()

  if (user === undefined) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-2 px-4 text-center">
        <Spinner label={waking ? 'Waking up the server' : 'Checking your session'} />
        {waking && <p className="max-w-sm text-sm text-slate-500">This can take up to a minute on the free hosting tier.</p>}
      </div>
    )
  }
  if (user === null) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <Outlet />
}
