import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { register, type Credentials } from '../api/auth'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/context'
import { ErrorMessage } from '../components/ErrorMessage'
import { buttonClasses, inputClasses, labelClasses } from '../components/ui'
import { AuthCard } from './AuthCard'

type Errors = Partial<Record<keyof Credentials | 'confirm', string>>

/** Mirrors the constraints on the backend's RegisterDto. */
function validate(username: string, password: string, confirm: string): Errors {
  const errors: Errors = {}
  if (!username) errors.username = 'Username is required.'
  else if (username.length > 32) errors.username = 'Username must be between 1 and 32 characters long.'
  else if (!/^[a-zA-Z0-9._-]+$/.test(username))
    errors.username = 'Username can only include alpha-numeric characters and ., _ and -.'
  if (password.length < 8 || password.length > 64)
    errors.password = 'Password must be between 8 and 64 characters long.'
  else if (password !== confirm) errors.confirm = 'Passwords do not match.'
  return errors
}

export function RegisterPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<Errors>({})

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: () => navigate('/login', { state: { registered: username } }),
  })

  if (user) return <Navigate to="/applications" replace />

  const serverErrors = mutation.error instanceof ApiError ? mutation.error.fieldErrors : {}
  const fieldError = (field: keyof Errors) =>
    errors[field] ?? (field !== 'confirm' ? serverErrors[field]?.join(' ') : undefined)
  const generalError =
    mutation.error && !(mutation.error instanceof ApiError && Object.keys(mutation.error.fieldErrors).length > 0)
      ? mutation.error.message
      : null

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const found = validate(username, password, confirm)
    setErrors(found)
    if (Object.keys(found).length === 0) mutation.mutate({ username, password })
  }

  function field(id: keyof Errors, label: string, value: string, onChange: (value: string) => void, type = 'text') {
    const message = fieldError(id)
    return (
      <div>
        <label htmlFor={id} className={labelClasses}>
          {label}
        </label>
        <input
          id={id}
          type={type}
          autoComplete={id === 'username' ? 'username' : 'new-password'}
          className={`${inputClasses} mt-1`}
          value={value}
          aria-invalid={message ? true : undefined}
          aria-describedby={message ? `${id}-error` : undefined}
          onChange={(event) => {
            onChange(event.target.value)
            setErrors((previous) => ({ ...previous, [id]: undefined }))
          }}
        />
        {message && (
          <p id={`${id}-error`} className="mt-1 text-sm text-rose-600">
            {message}
          </p>
        )}
      </div>
    )
  }

  return (
    <AuthCard
      title="Create an account"
      footer={
        <>
          Already registered?{' '}
          <Link to="/login" className="font-medium text-indigo-600 hover:text-indigo-500">
            Sign in
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={handleSubmit} className="space-y-4">
        <ErrorMessage>{generalError}</ErrorMessage>
        {field('username', 'Username', username, setUsername)}
        {field('password', 'Password', password, setPassword, 'password')}
        {field('confirm', 'Confirm password', confirm, setConfirm, 'password')}
        <p className="text-xs text-slate-500">
          Up to 32 characters (letters, digits, <code>.</code> <code>_</code> <code>-</code>). Password 8 to 64
          characters.
        </p>
        <button type="submit" className={`${buttonClasses.primary} w-full`} disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthCard>
  )
}
