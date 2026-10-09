import { Link, useLocation, useParams } from 'react-router-dom'
import { EvolutionChain } from '../components/EvolutionChain'
import { StatBars } from '../components/StatBars'
import { ErrorState, Notice, Spinner } from '../components/Status'
import { TypeList } from '../components/TypeBadge'
import { useAuth } from '../auth/useAuth'
import { usePokemonDetail } from '../hooks/useCatalog'
import { useSyncPokemon } from '../hooks/useLocalPokemon'
import { formatDexNumber, formatKg, formatMeters, formatName } from '../utils/format'

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams()
  const { data: pokemon, isPending, isError, error, refetch } = usePokemonDetail(idOrName.toLowerCase())
  const { session } = useAuth()
  const location = useLocation()
  const sync = useSyncPokemon()

  if (isPending) {
    return <Spinner label="Loading Pokémon" />
  }
  if (isError) {
    return (
      <section>
        <ErrorState error={error} onRetry={() => refetch()} />
        <Link to="/" className="btn">Back to the list</Link>
      </section>
    )
  }

  return (
    <article className="detail">
      <Link to="/" className="back-link">← All Pokémon</Link>

      <div className="detail__hero">
        <div className="detail__image">
          <img src={pokemon.imageUrl} alt={formatName(pokemon.name)} width={320} height={320} />
        </div>
        <div className="detail__summary">
          <p className="card__number">{formatDexNumber(pokemon.id)}</p>
          <h1>{formatName(pokemon.name)}</h1>
          {pokemon.category && <p className="detail__category">{pokemon.category}</p>}
          <TypeList types={pokemon.types} />
          {pokemon.description && <p className="detail__description">{pokemon.description}</p>}
          <dl className="facts facts--row">
            <div><dt>Height</dt><dd>{formatMeters(pokemon.heightM)}</dd></div>
            <div><dt>Weight</dt><dd>{formatKg(pokemon.weightKg)}</dd></div>
            <div><dt>Abilities</dt><dd>{pokemon.abilities.map(formatName).join(', ')}</dd></div>
          </dl>

          <div className="detail__actions">
            {session ? (
              <button type="button" className="btn btn--primary" onClick={() => sync.mutate(pokemon.name)} disabled={sync.isPending}>
                {sync.isPending ? 'Saving…' : 'Save to My Pokedex'}
              </button>
            ) : (
              <Link to="/login" state={{ from: location.pathname }} className="btn">Sign in to save it</Link>
            )}
          </div>
          {sync.isSuccess && (
            <Notice kind="success">
              Saved. <Link to={`/collection/${sync.data.id}`}>Add your own notes and tags →</Link>
            </Notice>
          )}
          {sync.isError && <Notice kind="error">{sync.error.message}</Notice>}
        </div>
      </div>

      <div className="detail__sections">
        <section className="panel">
          <h2>Base stats</h2>
          <StatBars stats={pokemon.stats} />
        </section>
        <section className="panel">
          <h2>Evolution</h2>
          <EvolutionChain chain={pokemon.evolutionChain} currentId={pokemon.id} />
        </section>
      </div>
    </article>
  )
}
