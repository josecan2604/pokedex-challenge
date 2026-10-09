import type { ProblemDetail } from './types'

export const API_BASE_URL: string = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

/**
 * Every non-2xx response becomes an ApiError carrying the status and, when the server
 * sent a problem body, its detail and per-field errors, so forms can show them inline.
 */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type TokenProvider = () => string | null
type UnauthorizedHandler = () => void

let tokenProvider: TokenProvider = () => null
let onUnauthorized: UnauthorizedHandler = () => {}

/** Wired once by AuthProvider, so the client stays free of React. */
export function configureAuth(provider: TokenProvider, unauthorizedHandler: UnauthorizedHandler): void {
  tokenProvider = provider
  onUnauthorized = unauthorizedHandler
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
}

export async function apiFetch<T>(path: string, { method = 'GET', body, signal }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = tokenProvider()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new ApiError(0, 'Cannot reach the server. Check that the gateway is running.')
  }

  if (response.status === 401 && token) {
    // Token expired or the gateway restarted with a new key: drop the session.
    onUnauthorized()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = {}
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    // Not JSON (e.g. a proxy error page): fall back to the status text.
  }
  return new ApiError(response.status, problem.detail ?? defaultMessage(response.status), problem.errors ?? {})
}

function defaultMessage(status: number): string {
  switch (status) {
    case 400:
      return 'The request is invalid.'
    case 401:
      return 'Please sign in to continue.'
    case 403:
      return 'You do not have permission to do this.'
    case 404:
      return 'Not found.'
    case 409:
      return 'This item was changed by someone else.'
    case 502:
    case 503:
    case 504:
      return 'The service is temporarily unavailable. Please try again.'
    default:
      return 'Something went wrong. Please try again.'
  }
}
