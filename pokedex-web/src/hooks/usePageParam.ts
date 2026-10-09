import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

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
