import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination'

function renderPagination(page: number, totalPages: number, totalElements: number, size = 20) {
  const onPageChange = vi.fn()
  const onSizeChange = vi.fn()
  render(
    <Pagination
      page={page}
      totalPages={totalPages}
      totalElements={totalElements}
      size={size}
      onPageChange={onPageChange}
      onSizeChange={onSizeChange}
    />,
  )
  return { onPageChange, onSizeChange }
}

describe('Pagination', () => {
  it('shows the range of the current page, one-based', async () => {
    const { onPageChange } = renderPagination(1, 3, 45)

    expect(screen.getByText('Showing 21–40 of 45')).toBeInTheDocument()
    expect(screen.getByText('Page 2 of 3')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Previous' }))
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(onPageChange.mock.calls).toEqual([[0], [2]])
  })

  it('cuts the range off at the last element', () => {
    renderPagination(2, 3, 45)

    expect(screen.getByText('Showing 41–45 of 45')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled()
  })

  it('disables both buttons without results', () => {
    renderPagination(0, 0, 0)

    expect(screen.getByText('No results')).toBeInTheDocument()
    expect(screen.getByText('Page 0 of 0')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
  })

  it('offers only the page sizes the backend allows', async () => {
    const { onSizeChange } = renderPagination(0, 1, 5)
    const select = screen.getByLabelText('Per page')

    expect([...select.querySelectorAll('option')].map((option) => option.value)).toEqual(['10', '20', '50'])
    await userEvent.selectOptions(select, '50')
    expect(onSizeChange).toHaveBeenCalledWith(50)
  })
})
