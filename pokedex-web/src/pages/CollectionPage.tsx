import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { LocalPokemonCard } from '../components/LocalPokemonCard'
import { Pagination } from '../components/Pagination'
import { CardGridSkeleton, EmptyState, ErrorState, Notice } from '../components/Status'
import { useAuth } from '../auth/useAuth'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { useLocalPokemonList, useSyncPokemon, useSyncRange } from '../hooks/useLocalPokemon'
import { usePageParam } from '../hooks/usePageParam'
import { formatName } from '../utils/format'

const PAGE_SIZE = 12
const MAX_RANGE = 50

export function CollectionPage() {
  const { session } = useAuth()
  const [params, setParams] = useSearchParams()
  const [page, setPage] = usePageParam()
  const [search, setSearch] = useState(params.get('q') ?? '')
  const debouncedSearch = useDebouncedValue(search)
  const { data, isPending, isError, error, refetch, isPlaceholderData } =
    useLocalPokemonList(page, PAGE_SIZE, debouncedSearch)

  const handleSearch = (value: string) => {
    setSearch(value)
    setParams((current) => {
      const updated = new URLSearchParams(current)
      updated.delete('page')
      if (value.trim()) {
        updated.set('q', value)
      } else {
        updated.delete('q')
      }
      return updated
    }, { replace: true })
  }

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>My Pokedex</h1>
          <p className="muted">Pokémon replicated locally, with your own names, regions, tags and notes.</p>
        </div>
        <div className="inline-form" role="search">
          <label htmlFor="collection-search" className="visually-hidden">Search my Pokedex</label>
          <input
            id="collection-search"
            type="search"
            placeholder="Search by name or tag"
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
          />
        </div>
      </div>

      {session ? (
        <SyncPanel />
      ) : (
        <Notice kind="info">
          <Link to="/login" state={{ from: '/collection' }}>Sign in</Link> to add Pokémon and edit their details.
        </Notice>
      )}

      {isPending && <CardGridSkeleton count={PAGE_SIZE} />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}
      {data && data.content.length === 0 && (
        <EmptyState>
          {debouncedSearch ? <>No Pokémon match “{debouncedSearch}”.</> : <>Your Pokedex is empty. <Link to="/">Browse</Link> and save some!</>}
        </EmptyState>
      )}
      {data && data.content.length > 0 && (
        <>
          <p className="muted result-count">{data.totalElements} saved</p>
          <div className={`grid${isPlaceholderData ? ' is-stale' : ''}`}>
            {data.content.map((pokemon) => <LocalPokemonCard key={pokemon.id} pokemon={pokemon} />)}
          </div>
          <Pagination page={page} totalPages={data.totalPages} onChange={setPage} disabled={isPlaceholderData} />
        </>
      )}
    </section>
  )
}

function SyncPanel() {
  const sync = useSyncPokemon()
  const syncRange = useSyncRange()
  const [idOrName, setIdOrName] = useState('')
  const [fromId, setFromId] = useState('1')
  const [toId, setToId] = useState('9')
  const [rangeError, setRangeError] = useState<string | null>(null)

  const handleSync = (event: FormEvent) => {
    event.preventDefault()
    const value = idOrName.trim().toLowerCase()
    if (value) {
      sync.mutate(value, { onSuccess: () => setIdOrName('') })
    }
  }

  const handleRange = (event: FormEvent) => {
    event.preventDefault()
    const from = Number(fromId)
    const to = Number(toId)
    if (!Number.isInteger(from) || !Number.isInteger(to) || from < 1 || to < from) {
      setRangeError('Enter two Pokedex numbers, the first not greater than the second.')
      return
    }
    if (to - from + 1 > MAX_RANGE) {
      setRangeError(`At most ${MAX_RANGE} Pokémon at a time.`)
      return
    }
    setRangeError(null)
    syncRange.mutate({ fromId: from, toId: to })
  }

  return (
    <div className="panel sync-panel">
      <form className="sync-panel__form" onSubmit={handleSync} aria-label="Add one Pokémon">
        <label htmlFor="sync-one">Add a Pokémon</label>
        <div className="inline-form">
          <input id="sync-one" placeholder="Name or number" value={idOrName} onChange={(e) => setIdOrName(e.target.value)} />
          <button type="submit" className="btn btn--primary" disabled={sync.isPending || !idOrName.trim()}>
            {sync.isPending ? 'Adding…' : 'Add'}
          </button>
        </div>
      </form>

      <form className="sync-panel__form" onSubmit={handleRange} aria-label="Add a range of Pokémon">
        <label htmlFor="sync-from">Add a range (max {MAX_RANGE})</label>
        <div className="inline-form">
          <input id="sync-from" type="number" min={1} value={fromId} onChange={(e) => setFromId(e.target.value)} aria-label="From number" />
          <span aria-hidden="true">–</span>
          <input type="number" min={1} value={toId} onChange={(e) => setToId(e.target.value)} aria-label="To number" />
          <button type="submit" className="btn" disabled={syncRange.isPending}>
            {syncRange.isPending ? 'Syncing…' : 'Sync range'}
          </button>
        </div>
      </form>

      <div className="sync-panel__feedback" aria-live="polite">
        {sync.isSuccess && (
          <Notice kind="success">
            {formatName(sync.data.name)} is in your Pokedex. <Link to={`/collection/${sync.data.id}`}>Edit it →</Link>
          </Notice>
        )}
        {sync.isError && <Notice kind="error">{sync.error.message}</Notice>}
        {syncRange.isSuccess && (
          <Notice kind="success">
            {syncRange.data.created} added, {syncRange.data.refreshed} refreshed.
          </Notice>
        )}
        {(rangeError || syncRange.isError) && <Notice kind="error">{rangeError ?? syncRange.error?.message}</Notice>}
      </div>
    </div>
  )
}
