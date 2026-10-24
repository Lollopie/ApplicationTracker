import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { STATUSES, type CreateApplication, type Status } from '../api/applications'
import { ApiError } from '../api/client'
import { statusLabels } from '../domain/status'
import { ErrorMessage } from './ErrorMessage'
import { buttonClasses, cardClasses, inputClasses, labelClasses } from './ui'

const MAX_TEXT_LENGTH = 500

export interface ApplicationFormValues {
  company: string
  position: string
  status: Status
  notes: string
  links: string
}

interface ApplicationFormProps {
  initialValues?: Partial<ApplicationFormValues>
  /** The status can only be chosen on create. Afterwards it changes through the status workflow. */
  showStatus: boolean
  submitLabel: string
  cancelTo: string
  submitting: boolean
  error: Error | null
  onSubmit: (values: CreateApplication) => void
}

type FieldName = keyof ApplicationFormValues

function validate(values: ApplicationFormValues): Partial<Record<FieldName, string>> {
  const errors: Partial<Record<FieldName, string>> = {}
  if (!values.company.trim()) errors.company = 'Company is required'
  if (!values.position.trim()) errors.position = 'Position is required'
  if (values.notes.length > MAX_TEXT_LENGTH) errors.notes = `At most ${MAX_TEXT_LENGTH} characters`
  if (values.links.length > MAX_TEXT_LENGTH) errors.links = `At most ${MAX_TEXT_LENGTH} characters`
  return errors
}

export function ApplicationForm({
  initialValues,
  showStatus,
  submitLabel,
  cancelTo,
  submitting,
  error,
  onSubmit,
}: ApplicationFormProps) {
  const [values, setValues] = useState<ApplicationFormValues>({
    company: '',
    position: '',
    status: 'SAVED',
    notes: '',
    links: '',
    ...initialValues,
  })
  const [clientErrors, setClientErrors] = useState<Partial<Record<FieldName, string>>>({})

  const serverFieldErrors = error instanceof ApiError ? error.fieldErrors : {}
  const fieldError = (field: FieldName) => clientErrors[field] ?? serverFieldErrors[field]?.join(' ')
  const generalError =
    error && !(error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) ? error.message : null

  function update(field: FieldName, value: string) {
    setValues((previous) => ({ ...previous, [field]: value }))
    setClientErrors((previous) => ({ ...previous, [field]: undefined }))
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const errors = validate(values)
    setClientErrors(errors)
    if (Object.keys(errors).length > 0) return
    onSubmit({
      company: values.company.trim(),
      position: values.position.trim(),
      status: values.status,
      notes: values.notes.trim() || undefined,
      links: values.links.trim() || undefined,
    })
  }

  function textField(field: 'company' | 'position', label: string, placeholder: string) {
    const message = fieldError(field)
    return (
      <div>
        <label htmlFor={field} className={labelClasses}>
          {label} <span className="text-rose-600">*</span>
        </label>
        <input
          id={field}
          className={`${inputClasses} mt-1`}
          value={values[field]}
          placeholder={placeholder}
          aria-invalid={message ? true : undefined}
          aria-describedby={message ? `${field}-error` : undefined}
          onChange={(event) => update(field, event.target.value)}
        />
        {message && (
          <p id={`${field}-error`} className="mt-1 text-sm text-rose-600">
            {message}
          </p>
        )}
      </div>
    )
  }

  function textArea(field: 'notes' | 'links', label: string, placeholder: string) {
    const message = fieldError(field)
    const length = values[field].length
    return (
      <div>
        <div className="flex items-baseline justify-between">
          <label htmlFor={field} className={labelClasses}>
            {label}
          </label>
          <span className={`text-xs ${length > MAX_TEXT_LENGTH ? 'text-rose-600' : 'text-slate-400'}`}>
            {length}/{MAX_TEXT_LENGTH}
          </span>
        </div>
        <textarea
          id={field}
          rows={4}
          className={`${inputClasses} mt-1`}
          value={values[field]}
          placeholder={placeholder}
          aria-invalid={message ? true : undefined}
          aria-describedby={message ? `${field}-error` : undefined}
          onChange={(event) => update(field, event.target.value)}
        />
        {message && (
          <p id={`${field}-error`} className="mt-1 text-sm text-rose-600">
            {message}
          </p>
        )}
      </div>
    )
  }

  return (
    <form noValidate onSubmit={handleSubmit} className={`${cardClasses} space-y-5 p-6`}>
      <ErrorMessage>{generalError}</ErrorMessage>
      <div className="grid gap-5 sm:grid-cols-2">
        {textField('company', 'Company', 'Acme Corp')}
        {textField('position', 'Position', 'Backend Engineer')}
      </div>
      {showStatus && (
        <div>
          <label htmlFor="status" className={labelClasses}>
            Initial status
          </label>
          <select
            id="status"
            className={`${inputClasses} mt-1 sm:max-w-xs`}
            value={values.status}
            onChange={(event) => update('status', event.target.value)}
          >
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {statusLabels[status]}
              </option>
            ))}
          </select>
        </div>
      )}
      {textArea('notes', 'Notes', 'Recruiter name, salary range, impressions…')}
      {textArea('links', 'Links', 'Job posting, company page… (one per line)')}
      <div className="flex justify-end gap-3">
        <Link to={cancelTo} className={buttonClasses.secondary}>
          Cancel
        </Link>
        <button type="submit" className={buttonClasses.primary} disabled={submitting}>
          {submitting ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
