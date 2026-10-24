const URL_PATTERN = /(https?:\/\/[^\s]+)/g

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
