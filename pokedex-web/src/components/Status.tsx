import type { ReactNode } from 'react'
import { ApiError } from '../api/client'

export function CardGridSkeleton({ count = 12 }: { count?: number }) {
  return (
    <div className="grid" aria-busy="true" aria-label="Loading">
      {Array.from({ length: count }, (_, i) => <div key={i} className="card card--skeleton" />)}
    </div>
  )
}

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="spinner" role="status">
      <span className="spinner__circle" aria-hidden="true" />
      <span className="visually-hidden">{label}</span>
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof Error ? error.message : 'Something went wrong.'
  const notFound = error instanceof ApiError && error.status === 404
  return (
    <div className="notice notice--error" role="alert">
      <p>{notFound ? 'We could not find that.' : message}</p>
      {onRetry && !notFound && (
        <button type="button" className="btn" onClick={onRetry}>Try again</button>
      )}
    </div>
  )
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}

export function Notice({ kind, children }: { kind: 'success' | 'error' | 'info'; children: ReactNode }) {
  return (
    <div className={`notice notice--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
