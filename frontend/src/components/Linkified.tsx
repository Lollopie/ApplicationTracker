// Parentheses are only kept when balanced (e.g. Wikipedia URLs), and the last character
// may not be punctuation, so "(see https://a.com)." links just "https://a.com".
// Inner groups must stay non-capturing: split() relies on exactly one capturing group.
const URL_PATTERN = /(https?:\/\/(?:[^\s()]|\([^\s()]*\))*(?:[^\s()<>.,;:!?'"]|\([^\s()]*\)))/g

/** Renders free text with http(s) URLs as links. Other schemes (javascript:, data:) stay plain text. */
export function Linkified({ text }: { text: string }) {
  const parts = text.split(URL_PATTERN)
  return (
    <span className="whitespace-pre-wrap break-words">
      {parts.map((part, index) =>
        index % 2 === 1 ? (
          <a
            key={index}
            href={part}
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-600 underline hover:text-indigo-500"
          >
            {part}
          </a>
        ) : (
          part
        ),
      )}
    </span>
  )
}
