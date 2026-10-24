import { useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/context'
import { HOME_URL } from '../config'

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await logout()
    } catch {
      // The session is cleared locally either way.
    } finally {
      setLoggingOut(false)
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/applications" className="flex items-center gap-2 text-lg font-semibold text-slate-900">
            <span className="flex size-8 items-center justify-center rounded-md bg-indigo-600 text-sm text-white">JT</span>
            Job Tracker
          </Link>
          <div className="flex items-center gap-4 text-sm">
            {HOME_URL && (
              // A plain link on purpose: the home page lives outside this app's router.
              <a href={HOME_URL} className="text-slate-600 hover:text-slate-900">
                Home
              </a>
            )}
            <span className="hidden text-slate-600 sm:inline">
              Signed in as <strong className="font-medium text-slate-900">{user?.username}</strong>
            </span>
            <button
              type="button"
              onClick={handleLogout}
              disabled={loggingOut}
              className="rounded-md px-3 py-1.5 font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-60"
            >
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
