import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Linkified } from './Linkified'

function linkHrefs() {
  return screen.queryAllByRole('link').map((link) => link.getAttribute('href'))
}

describe('Linkified', () => {
  it.each([
    ['https://a.com', 'https://a.com'],
    ['see https://a.com/path?q=1&b=2#top', 'https://a.com/path?q=1&b=2#top'],
    ['https://a.com/dir/', 'https://a.com/dir/'],
    ['https://en.wikipedia.org/wiki/Foo_(bar)', 'https://en.wikipedia.org/wiki/Foo_(bar)'],
    ['(see https://a.com).', 'https://a.com'],
    ['Visit https://a.com, then https://b.com.', 'https://a.com'],
    ['Really https://a.com?!', 'https://a.com'],
    ['"https://a.com"', 'https://a.com'],
    ['<https://a.com>', 'https://a.com'],
    ['[docs](https://a.com/docs)', 'https://a.com/docs'],
  ])('links %j as %s', (text, href) => {
    render(<Linkified text={text} />)
    expect(linkHrefs()[0]).toBe(href)
  })

  it('keeps trailing punctuation as plain text', () => {
    const { container } = render(<Linkified text="(see https://a.com)." />)
    expect(container.textContent).toBe('(see https://a.com).')
  })

  it('links every URL in the text', () => {
    render(<Linkified text={'https://a.com\nhttps://b.com'} />)
    expect(linkHrefs()).toEqual(['https://a.com', 'https://b.com'])
  })

  it('does not link other schemes', () => {
    render(<Linkified text="javascript:alert(1) https://" />)
    expect(linkHrefs()).toEqual([])
  })
})
