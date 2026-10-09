export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface PokemonSummary {
  id: number
  name: string
  spriteUrl: string
  category?: string
  weightKg: number
  types: string[]
  abilities: string[]
}

export interface PokemonStat {
  name: string
  baseValue: number
}

export interface EvolutionStage {
  id: number
  name: string
  stage: number
  evolvesFromId?: number
  spriteUrl: string
}

export interface PokemonDetail {
  id: number
  name: string
  imageUrl: string
  spriteUrl: string
  category?: string
  description?: string
  heightM: number
  weightKg: number
  types: string[]
  abilities: string[]
  stats: PokemonStat[]
  evolutionChain: EvolutionStage[]
}

export interface LocalPokemon {
  id: number
  name: string
  spriteUrl?: string
  category?: string
  weightKg: number
  heightM: number
  types: string[]
  abilities: string[]
  localizedName?: string
  region?: string
  tags: string[]
  notes?: string
  version: number
  syncedAt: string
  updatedAt: string
}

export interface CustomFields {
  localizedName: string | null
  region: string | null
  tags: string[]
  notes: string | null
}

export interface SyncRangeResult {
  created: number
  refreshed: number
  pokemon: LocalPokemon[]
}

export interface RegisteredUser {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
}

export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: Record<string, string>
}

export interface TokenResponse {
  access_token?: string
  token_type?: string
  expires_in?: number
  error?: string
  error_description?: string
}
