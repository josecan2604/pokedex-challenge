import { API_BASE_URL, ApiError, apiFetch } from './client'
import type {
  CustomFields,
  LocalPokemon,
  Page,
  PokemonDetail,
  PokemonSummary,
  RegisteredUser,
  SyncRangeResult,
  TokenResponse,
} from './types'

/** Endpoint functions: the only place that knows URLs and HTTP verbs. */

export const catalogApi = {
  browse: (page: number, size: number, signal?: AbortSignal) =>
    apiFetch<Page<PokemonSummary>>(`/api/pokemon?page=${page}&size=${size}`, { signal }),

  detail: (idOrName: string, signal?: AbortSignal) =>
    apiFetch<PokemonDetail>(`/api/pokemon/${encodeURIComponent(idOrName)}`, { signal }),
}

export const localPokemonApi = {
  list: (page: number, size: number, search: string, signal?: AbortSignal) => {
    const query = new URLSearchParams({ page: String(page), size: String(size) })
    if (search.trim()) {
      query.set('search', search.trim())
    }
    return apiFetch<Page<LocalPokemon>>(`/api/local-pokemon?${query}`, { signal })
  },

  get: (id: number, signal?: AbortSignal) => apiFetch<LocalPokemon>(`/api/local-pokemon/${id}`, { signal }),

  sync: (idOrName: string) =>
    apiFetch<LocalPokemon>('/api/local-pokemon/sync', { method: 'POST', body: { idOrName } }),

  syncRange: (fromId: number, toId: number) =>
    apiFetch<SyncRangeResult>('/api/local-pokemon/sync/range', { method: 'POST', body: { fromId, toId } }),

  update: (id: number, fields: CustomFields, version: number) =>
    apiFetch<LocalPokemon>(`/api/local-pokemon/${id}`, { method: 'PUT', body: { ...fields, version } }),

  remove: (id: number) => apiFetch<void>(`/api/local-pokemon/${id}`, { method: 'DELETE' }),
}

export const authApi = {
  register: (username: string, email: string, password: string) =>
    apiFetch<RegisteredUser>('/auth/register', { method: 'POST', body: { username, email, password } }),

  /** OAuth2 password grant: /oauth2/token is routed by the gateway to pokedex-auth (form-encoded, per RFC 6749). */
  async token(username: string, password: string): Promise<string> {
    let response: Response
    try {
      response = await fetch(`${API_BASE_URL}/oauth2/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded', Accept: 'application/json' },
        body: new URLSearchParams({ grant_type: 'password', username, password }),
      })
    } catch {
      throw new ApiError(0, 'Cannot reach the server. Check that the gateway is running.')
    }
    const body = (await response.json().catch(() => ({}))) as TokenResponse
    if (!response.ok || !body.access_token) {
      const message = body.error === 'invalid_grant'
        ? 'Invalid username or password.'
        : body.error_description ?? 'Sign in failed. Please try again.'
      throw new ApiError(response.status, message)
    }
    return body.access_token
  },
}
