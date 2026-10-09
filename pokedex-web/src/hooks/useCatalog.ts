import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { catalogApi } from '../api/pokedexApi'

export const catalogKeys = {
  page: (page: number, size: number) => ['catalog', 'page', page, size] as const,
  detail: (idOrName: string) => ['catalog', 'detail', idOrName] as const,
}

const CATALOG_STALE_TIME = 10 * 60 * 1000

export function useCatalogPage(page: number, size: number) {
  return useQuery({
    queryKey: catalogKeys.page(page, size),
    queryFn: ({ signal }) => catalogApi.browse(page, size, signal),
    placeholderData: keepPreviousData,
    staleTime: CATALOG_STALE_TIME,
  })
}

export function usePokemonDetail(idOrName: string) {
  return useQuery({
    queryKey: catalogKeys.detail(idOrName),
    queryFn: ({ signal }) => catalogApi.detail(idOrName, signal),
    staleTime: CATALOG_STALE_TIME,
    enabled: idOrName.length > 0,
  })
}
