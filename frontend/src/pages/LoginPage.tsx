import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/context'
import { ErrorMessage, SuccessMessage } from '../components/ErrorMessage'
import { buttonClasses, inputClasses, labelClasses } from '../components/ui'
import { AuthCard } from './AuthCard'

interface LocationState {
  from?: string
  registered?: string
}

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const state = (useLocation().state ?? {}) as LocationState
  const [username, setUsername] = useState(state.registered ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const target = state.from ?? '/applications'
  if (user) return <Navigate to={target} replace />

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!username || !password) {
      setError('Enter your username and password.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await login({ username, password })
      navigate(target, { replace: true })
    } catch (caught) {
      setError(
        caught instanceof ApiError && caught.status === 401
          ? 'Invalid username or password.'
          : caught instanceof Error
            ? caught.message
            : 'Login failed.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Sign in"
      footer={
        <>
          No account yet?{' '}
          <Link to="/register" className="font-medium text-indigo-600 hover:text-indigo-500">
            Create one
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={handleSubmit} className="space-y-4">
        {state.registered && !error && (
          <SuccessMessage>Account created. You can sign in now.</SuccessMessage>
        )}
        <ErrorMessage>{error}</ErrorMessage>
        <div>
          <label htmlFor="username" className={labelClasses}>
            Username
          </label>
          <input
            id="username"
            autoComplete="username"
            className={`${inputClasses} mt-1`}
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="password" className={labelClasses}>
            Password
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            className={`${inputClasses} mt-1`}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>
        <button type="submit" className={`${buttonClasses.primary} w-full`} disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </AuthCard>
  )
}
