import type { ReactNode } from 'react'
import { cardClasses } from '../components/ui'

export function AuthCard({ title, children, footer }: { title: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 py-12">
      <div className="mb-8 flex items-center gap-2 text-xl font-semibold text-slate-900">
        <span className="flex size-9 items-center justify-center rounded-md bg-indigo-600 text-sm text-white">JT</span>
        Job Tracker
      </div>
      <div className={`${cardClasses} w-full max-w-sm p-6`}>
        <h1 className="mb-6 text-lg font-semibold text-slate-900">{title}</h1>
        {children}
      </div>
      <p className="mt-6 text-sm text-slate-600">{footer}</p>
    </div>
  )
}
