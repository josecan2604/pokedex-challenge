import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { PokemonCard } from '../components/PokemonCard'
import { Pagination } from '../components/Pagination'
import { CardGridSkeleton, ErrorState } from '../components/Status'
import { useCatalogPage } from '../hooks/useCatalog'
import { usePageParam } from '../hooks/usePageParam'

const PAGE_SIZE = 24

/** US01: paginated catalog with sprite, category, weight and abilities. */
export function BrowsePage() {
  const [page, setPage] = usePageParam()
  const { data, isPending, isError, error, refetch, isPlaceholderData } = useCatalogPage(page, PAGE_SIZE)
  const [lookup, setLookup] = useState('')
  const navigate = useNavigate()

  const handleLookup = (event: FormEvent) => {
    event.preventDefault()
    const value = lookup.trim().toLowerCase()
    if (value) {
      navigate(`/pokemon/${encodeURIComponent(value)}`)
    }
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Browse Pokémon</h1>
          <p className="muted">
            {data ? `${data.totalElements.toLocaleString()} Pokémon from PokeAPI` : 'Live data from PokeAPI'}
          </p>
        </div>
        <form className="inline-form" role="search" onSubmit={handleLookup}>
          <label htmlFor="lookup" className="visually-hidden">Find by name or number</label>
          <input
            id="lookup"
            type="search"
            placeholder="Name or number, e.g. pikachu"
            value={lookup}
            onChange={(e) => setLookup(e.target.value)}
          />
          <button type="submit" className="btn btn--primary">Go</button>
        </form>
      </div>

      {isPending && <CardGridSkeleton count={PAGE_SIZE} />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {data && (
        <>
          <div className={`grid${isPlaceholderData ? ' is-stale' : ''}`} aria-busy={isPlaceholderData}>
            {data.content.map((pokemon) => <PokemonCard key={pokemon.id} pokemon={pokemon} />)}
          </div>
          <Pagination page={page} totalPages={data.totalPages} onChange={setPage} disabled={isPlaceholderData} />
        </>
      )}
    </section>
  )
}
