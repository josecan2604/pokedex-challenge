import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiFetch, configureAuth } from './client'
import { jsonResponse } from '../test/utils'

describe('apiFetch', () => {
  const fetchMock = vi.fn<typeof fetch>()
  const onUnauthorized = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    configureAuth(() => null, onUnauthorized)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
    onUnauthorized.mockReset()
  })

  it('returns parsed JSON on success', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ id: 25 }))

    await expect(apiFetch('/api/local-pokemon/25')).resolves.toEqual({ id: 25 })
  })

  it('sends the bearer token and a JSON body', async () => {
    configureAuth(() => 'abc', onUnauthorized)
    fetchMock.mockResolvedValue(jsonResponse({}))

    await apiFetch('/api/local-pokemon/25', { method: 'PUT', body: { region: 'Kanto' } })

    const [, init] = fetchMock.mock.calls[0]
    expect(init?.headers).toMatchObject({ Authorization: 'Bearer abc', 'Content-Type': 'application/json' })
    expect(init?.body).toBe('{"region":"Kanto"}')
  })

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiFetch('/api/local-pokemon/25', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('turns a problem body into an ApiError with field errors', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ status: 400, detail: 'Invalid', errors: { tags: 'bad tag' } }, 400))

    const error = await apiFetch('/x').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Invalid', fieldErrors: { tags: 'bad tag' } })
  })

  it('uses a friendly default message when the body is not JSON', async () => {
    fetchMock.mockResolvedValue(new Response('<html>Bad Gateway</html>', { status: 502 }))

    await expect(apiFetch('/x')).rejects.toMatchObject({ status: 502, message: expect.stringContaining('temporarily unavailable') })
  })

  it('signs the user out when an authenticated request gets 401', async () => {
    configureAuth(() => 'expired', onUnauthorized)
    fetchMock.mockResolvedValue(jsonResponse({ status: 401 }, 401))

    await expect(apiFetch('/x')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('does not sign out on 401 when no token was sent', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ status: 401 }, 401))

    await expect(apiFetch('/x')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('reports network failures as status 0', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(apiFetch('/x')).rejects.toMatchObject({ status: 0, message: expect.stringContaining('Cannot reach') })
  })
})
