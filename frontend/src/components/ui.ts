const base =
  'inline-flex items-center justify-center gap-2 rounded-md px-3.5 py-2 text-sm font-medium shadow-sm transition focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60'

export const buttonClasses = {
  primary: `${base} bg-indigo-600 text-white hover:bg-indigo-500 focus-visible:outline-indigo-600`,
  secondary: `${base} border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 focus-visible:outline-indigo-600`,
  danger: `${base} bg-rose-600 text-white hover:bg-rose-500 focus-visible:outline-rose-600`,
}

export const inputClasses =
  'block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/30 aria-invalid:border-rose-400 aria-invalid:ring-rose-200'

export const labelClasses = 'block text-sm font-medium text-slate-700'

export const cardClasses = 'rounded-lg border border-slate-200 bg-white shadow-sm'
