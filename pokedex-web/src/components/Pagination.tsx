interface PaginationProps {
  page: number
  totalPages: number
  onChange: (page: number) => void
  disabled?: boolean
}

/** Zero-based page in, one-based page shown to people. */
export function Pagination({ page, totalPages, onChange, disabled = false }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  const isFirst = page <= 0
  const isLast = page >= totalPages - 1
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" className="btn" onClick={() => onChange(0)} disabled={disabled || isFirst} aria-label="First page">«</button>
      <button type="button" className="btn" onClick={() => onChange(page - 1)} disabled={disabled || isFirst}>Previous</button>
      <span className="pagination__status" aria-live="polite">
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" className="btn" onClick={() => onChange(page + 1)} disabled={disabled || isLast}>Next</button>
      <button type="button" className="btn" onClick={() => onChange(totalPages - 1)} disabled={disabled || isLast} aria-label="Last page">»</button>
    </nav>
  )
}
