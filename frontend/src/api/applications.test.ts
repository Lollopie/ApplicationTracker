import { describe, expect, it } from 'vitest'
import { toQueryString } from './applications'

describe('toQueryString', () => {
  it('leaves out empty filters, which the backend would reject', () => {
    expect(toQueryString({ company: '', position: '   ' })).toBe('')
  })

  it('serializes filters, sort and paging', () => {
    expect(
      toQueryString({
        currentStatus: 'APPLIED',
        company: ' Acme ',
        sort: { field: 'company', direction: 'asc' },
        page: 2,
        size: 50,
      }),
    ).toBe('?currentStatus=APPLIED&company=Acme&sort=company%2Casc&page=2&size=50')
  })
})
