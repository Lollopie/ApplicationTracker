import { useEffect, useEffectEvent, useRef, type ReactNode } from 'react'
import { buttonClasses } from './ui'

interface ConfirmDialogProps {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({ open, title, children, confirmLabel, busy, onConfirm, onCancel }: ConfirmDialogProps) {
  const cancelRef = useRef<HTMLButtonElement>(null)

  // While the action runs, the dialog can't be dismissed: it closes when the action finishes.
  const cancel = () => {
    if (!busy) onCancel()
  }
  // Reads the latest props without re-running the effect, which would move focus back to Cancel on every render.
  const onKeyDown = useEffectEvent((event: KeyboardEvent) => {
    if (event.key === 'Escape') cancel()
  })

  useEffect(() => {
    if (!open) return
    cancelRef.current?.focus()
    const listener = (event: KeyboardEvent) => onKeyDown(event)
    window.addEventListener('keydown', listener)
    return () => window.removeEventListener('keydown', listener)
  }, [open])

  if (!open) return null
  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/40 p-4" onClick={cancel}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-dialog-title" className="text-lg font-semibold text-slate-900">
          {title}
        </h2>
        <div className="mt-2 text-sm text-slate-600">{children}</div>
        <div className="mt-6 flex justify-end gap-3">
          <button ref={cancelRef} type="button" className={buttonClasses.secondary} disabled={busy} onClick={cancel}>
            Cancel
          </button>
          <button type="button" className={buttonClasses.danger} disabled={busy} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
