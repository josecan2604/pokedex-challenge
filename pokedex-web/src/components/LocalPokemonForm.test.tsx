import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LocalPokemonForm } from './LocalPokemonForm'
import { jsonResponse, localPikachu, renderWithProviders } from '../test/utils'

describe('LocalPokemonForm (US04)', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => vi.stubGlobal('fetch', fetchMock))
  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
  })

  it('starts with the current values', () => {
    renderWithProviders(<LocalPokemonForm pokemon={localPikachu} onReload={() => {}} />)

    expect(screen.getByLabelText('Localized name')).toHaveValue('Pikachu')
    expect(screen.getByLabelText('Region')).toHaveValue('Kanto')
    expect(screen.getByLabelText('Tags')).toHaveValue('mascot')
  })

  it('blocks invalid input on the client without calling the API', async () => {
    renderWithProviders(<LocalPokemonForm pokemon={localPikachu} onReload={() => {}} />)

    const tags = screen.getByLabelText('Tags')
    await userEvent.clear(tags)
    await userEvent.type(tags, 'bad tag!')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(screen.getByText(/"bad tag!" must be/)).toBeInTheDocument()
    expect(tags).toHaveAttribute('aria-invalid', 'true')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('sends normalized fields with the version it was loaded with', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ ...localPikachu, version: 4, tags: ['fan-favorite', 'mascot'] }))
    renderWithProviders(<LocalPokemonForm pokemon={localPikachu} onReload={() => {}} />)

    const tags = screen.getByLabelText('Tags')
    await userEvent.clear(tags)
    await userEvent.type(tags, 'Mascot, fan-favorite')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Saved.')).toBeInTheDocument()
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toMatch(/\/api\/local-pokemon\/25$/)
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(init?.body as string)).toEqual({
      localizedName: 'Pikachu',
      region: 'Kanto',
      tags: ['mascot', 'fan-favorite'],
      notes: null,
      version: 3,
    })
  })

  it('shows server field errors next to the field (400)', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ status: 400, detail: 'Invalid', errors: { region: 'Unknown region' } }, 400))
    renderWithProviders(<LocalPokemonForm pokemon={localPikachu} onReload={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(await screen.findByText('Unknown region')).toBeInTheDocument()
  })

  it('offers to reload when someone else saved first (409)', async () => {
    const onReload = vi.fn()
    fetchMock.mockResolvedValue(jsonResponse({ status: 409, detail: 'Conflict' }, 409))
    renderWithProviders(<LocalPokemonForm pokemon={localPikachu} onReload={onReload} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Load the latest version' }))

    expect(onReload).toHaveBeenCalledOnce()
  })
})
