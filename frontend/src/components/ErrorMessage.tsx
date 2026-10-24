import type { ReactNode } from 'react'

export function ErrorMessage({ children }: { children: ReactNode }) {
  if (!children) return null
  return (
    <div role="alert" className="rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">
      {children}
    </div>
  )
}

export function SuccessMessage({ children }: { children: ReactNode }) {
  if (!children) return null
  return (
    <div role="status" className="rounded-md border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
      {children}
    </div>
  )
}
