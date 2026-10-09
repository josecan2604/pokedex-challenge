import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { LocalPokemon, PokemonSummary } from '../api/types'

export function renderWithProviders(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return {
    queryClient,
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </QueryClientProvider>,
    ),
  }
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}

export const bulbasaur: PokemonSummary = {
  id: 1,
  name: 'bulbasaur',
  spriteUrl: 'https://img/1.png',
  category: 'Seed Pokémon',
  weightKg: 6.9,
  types: ['grass', 'poison'],
  abilities: ['overgrow', 'chlorophyll'],
}

export const localPikachu: LocalPokemon = {
  id: 25,
  name: 'pikachu',
  spriteUrl: 'https://img/25.png',
  category: 'Mouse Pokémon',
  weightKg: 6,
  heightM: 0.4,
  types: ['electric'],
  abilities: ['static'],
  localizedName: 'Pikachu',
  region: 'Kanto',
  tags: ['mascot'],
  notes: undefined,
  version: 3,
  syncedAt: '2026-10-09T12:00:00Z',
  updatedAt: '2026-10-09T12:00:00Z',
}
