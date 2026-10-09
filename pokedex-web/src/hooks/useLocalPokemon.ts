import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { localPokemonApi } from '../api/pokedexApi'
import type { CustomFields, LocalPokemon } from '../api/types'

export const localKeys = {
  all: ['local'] as const,
  list: (page: number, size: number, search: string) => ['local', 'list', page, size, search] as const,
  detail: (id: number) => ['local', 'detail', id] as const,
}

export function useLocalPokemonList(page: number, size: number, search: string) {
  return useQuery({
    queryKey: localKeys.list(page, size, search),
    queryFn: ({ signal }) => localPokemonApi.list(page, size, search, signal),
    placeholderData: keepPreviousData,
  })
}

export function useLocalPokemon(id: number) {
  return useQuery({
    queryKey: localKeys.detail(id),
    queryFn: ({ signal }) => localPokemonApi.get(id, signal),
    enabled: Number.isInteger(id) && id > 0,
  })
}

/** US03: copy one Pokemon from PokeAPI into the local store. */
export function useSyncPokemon() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (idOrName: string) => localPokemonApi.sync(idOrName),
    onSuccess: (pokemon) => {
      queryClient.setQueryData(localKeys.detail(pokemon.id), pokemon)
      return queryClient.invalidateQueries({ queryKey: ['local', 'list'] })
    },
  })
}

export function useSyncRange() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ fromId, toId }: { fromId: number; toId: number }) => localPokemonApi.syncRange(fromId, toId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: localKeys.all }),
  })
}

/** US04: update the proprietary fields, with optimistic-locking version. */
export function useUpdateLocalPokemon(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ fields, version }: { fields: CustomFields; version: number }) =>
      localPokemonApi.update(id, fields, version),
    onSuccess: (updated: LocalPokemon) => {
      // Write the server's answer straight into the cache: no refetch needed for the detail.
      queryClient.setQueryData(localKeys.detail(id), updated)
      return queryClient.invalidateQueries({ queryKey: ['local', 'list'] })
    },
  })
}

export function useDeleteLocalPokemon() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => localPokemonApi.remove(id),
    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: localKeys.detail(id) })
      return queryClient.invalidateQueries({ queryKey: ['local', 'list'] })
    },
  })
}
