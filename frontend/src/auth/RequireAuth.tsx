import { Navigate, Outlet, useLocation } from 'react-router'
import { Spinner } from '../components/Spinner'
import { useAuth } from './context'

export function RequireAuth() {
  const { user } = useAuth()
  const location = useLocation()

  if (user === undefined) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner label="Checking your session" />
      </div>
    )
  }
  if (user === null) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  return <Outlet />
}
