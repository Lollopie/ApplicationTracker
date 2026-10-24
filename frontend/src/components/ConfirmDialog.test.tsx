import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'

function dialog(props: { busy?: boolean; onCancel?: () => void; onConfirm?: () => void } = {}) {
  return (
    <ConfirmDialog
      open
      title="Delete application?"
      confirmLabel="Delete"
      busy={props.busy}
      onConfirm={props.onConfirm ?? (() => {})}
      onCancel={props.onCancel ?? (() => {})}
    >
      This permanently deletes it.
    </ConfirmDialog>
  )
}

describe('ConfirmDialog', () => {
  it('focuses Cancel when it opens', () => {
    render(dialog())
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus()
  })

  it('keeps the focus where it is when the parent re-renders with new callbacks', () => {
    const { rerender } = render(dialog())
    const confirm = screen.getByRole('button', { name: 'Delete' })
    confirm.focus()

    // Parents pass inline arrow functions, so every render brings a new onCancel.
    rerender(dialog({ onCancel: () => {} }))

    expect(confirm).toHaveFocus()
  })

  it('cancels on Escape, with the latest callback', async () => {
    const first = vi.fn()
    const latest = vi.fn()
    const { rerender } = render(dialog({ onCancel: first }))
    rerender(dialog({ onCancel: latest }))

    await userEvent.keyboard('{Escape}')

    expect(first).not.toHaveBeenCalled()
    expect(latest).toHaveBeenCalledOnce()
  })

  it('cancels on a click on the backdrop but not inside the dialog', async () => {
    const onCancel = vi.fn()
    render(dialog({ onCancel }))

    await userEvent.click(screen.getByText('This permanently deletes it.'))
    expect(onCancel).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('dialog').parentElement!)
    expect(onCancel).toHaveBeenCalledOnce()
  })

  it('cannot be dismissed while the action runs', async () => {
    const onCancel = vi.fn()
    const onConfirm = vi.fn()
    render(dialog({ busy: true, onCancel, onConfirm }))

    await userEvent.keyboard('{Escape}')
    await userEvent.click(screen.getByRole('dialog').parentElement!)

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Delete' })).toBeDisabled()
    expect(onCancel).not.toHaveBeenCalled()
  })
})
