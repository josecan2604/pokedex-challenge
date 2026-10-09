import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

/**
 * Keeps the current page in the URL (?page=3, one-based for people) so it survives reloads,
 * can be shared, and works with the back button. Returns a zero-based page for the API.
 */
export function usePageParam(): [number, (page: number) => void] {
  const [params, setParams] = useSearchParams()
  const parsed = Number.parseInt(params.get('page') ?? '1', 10)
  const page = Number.isFinite(parsed) && parsed > 0 ? parsed - 1 : 0

  const setPage = useCallback((next: number) => {
    setParams((current) => {
      const updated = new URLSearchParams(current)
      if (next <= 0) {
        updated.delete('page')
      } else {
        updated.set('page', String(next + 1))
      }
      return updated
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [setParams])

  return [page, setPage]
}
