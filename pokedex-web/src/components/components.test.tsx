import { describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { EvolutionChain } from './EvolutionChain'
import { Pagination } from './Pagination'
import { PokemonCard } from './PokemonCard'
import { StatBars } from './StatBars'
import { bulbasaur, renderWithProviders } from '../test/utils'

describe('PokemonCard', () => {
  it('shows sprite data, category, weight and abilities (US01)', () => {
    renderWithProviders(<PokemonCard pokemon={bulbasaur} />)

    expect(screen.getByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument()
    expect(screen.getByText('#0001')).toBeInTheDocument()
    expect(screen.getByText('Seed Pokémon')).toBeInTheDocument()
    expect(screen.getByText('6.9 kg')).toBeInTheDocument()
    expect(screen.getByText('Overgrow, Chlorophyll')).toBeInTheDocument()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/pokemon/bulbasaur')
  })
})

describe('Pagination', () => {
  it('is hidden when there is a single page', () => {
    const { container } = renderWithProviders(<Pagination page={0} totalPages={1} onChange={() => {}} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('shows one-based pages and moves with the buttons', async () => {
    const onChange = vi.fn()
    renderWithProviders(<Pagination page={0} totalPages={5} onChange={onChange} />)

    expect(screen.getByText('Page 1 of 5')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Next' }))
    await userEvent.click(screen.getByRole('button', { name: 'Last page' }))
    expect(onChange.mock.calls).toEqual([[1], [4]])
  })
})

describe('StatBars', () => {
  it('lists every stat and the total', () => {
    renderWithProviders(<StatBars stats={[{ name: 'hp', baseValue: 45 }, { name: 'special-attack', baseValue: 65 }]} />)

    expect(screen.getByRole('meter', { name: 'HP' })).toHaveAttribute('aria-valuenow', '45')
    expect(screen.getByRole('meter', { name: 'Sp. Atk' })).toBeInTheDocument()
    expect(screen.getByText('110')).toBeInTheDocument()
  })
})

describe('EvolutionChain', () => {
  it('groups branching evolutions in the same stage and marks the current one', () => {
    renderWithProviders(
      <EvolutionChain
        currentId={134}
        chain={[
          { id: 133, name: 'eevee', stage: 1, spriteUrl: 'a' },
          { id: 134, name: 'vaporeon', stage: 2, evolvesFromId: 133, spriteUrl: 'b' },
          { id: 135, name: 'jolteon', stage: 2, evolvesFromId: 133, spriteUrl: 'c' },
        ]}
      />,
    )

    const stage2 = screen.getByRole('list', { name: 'Stage 2' })
    expect(within(stage2).getAllByRole('link')).toHaveLength(2)
    expect(screen.getByRole('link', { name: 'Vaporeon' })).toHaveAttribute('aria-current', 'page')
  })

  it('says when a Pokemon does not evolve', () => {
    renderWithProviders(<EvolutionChain currentId={128} chain={[{ id: 128, name: 'tauros', stage: 1, spriteUrl: 'a' }]} />)
    expect(screen.getByText('This Pokémon does not evolve.')).toBeInTheDocument()
  })
})
