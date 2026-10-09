import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState, Notice, Spinner } from '../components/Status'
import { TypeList } from '../components/TypeBadge'
import { LocalPokemonForm } from '../components/LocalPokemonForm'
import { useAuth } from '../auth/useAuth'
import { useDeleteLocalPokemon, useLocalPokemon } from '../hooks/useLocalPokemon'
import { formatDexNumber, formatKg, formatMeters, formatName } from '../utils/format'

/** US04: view a local Pokemon and edit its proprietary fields. Admins can delete it. */
export function LocalPokemonPage() {
  const id = Number(useParams().id)
  const { session, isAdmin } = useAuth()
  const navigate = useNavigate()
  const { data: pokemon, isPending, isError, error, refetch } = useLocalPokemon(id)
  const remove = useDeleteLocalPokemon()

  if (!Number.isInteger(id) || id <= 0) {
    return <ErrorState error={new Error('That is not a valid Pokedex number.')} />
  }
  if (isPending) {
    return <Spinner label="Loading Pokémon" />
  }
  if (isError) {
    return (
      <section>
        <ErrorState error={error} onRetry={() => refetch()} />
        <Link to="/collection" className="btn">Back to My Pokedex</Link>
      </section>
    )
  }

  const handleDelete = () => {
    if (window.confirm(`Remove ${formatName(pokemon.name)} from your Pokedex? Your notes and tags will be lost.`)) {
      remove.mutate(pokemon.id, { onSuccess: () => navigate('/collection', { replace: true }) })
    }
  }

  return (
    <article className="detail">
      <Link to="/collection" className="back-link">← My Pokedex</Link>

      <div className="detail__hero detail__hero--compact">
        <div className="detail__image detail__image--small">
          {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt={formatName(pokemon.name)} width={160} height={160} />}
        </div>
        <div className="detail__summary">
          <p className="card__number">{formatDexNumber(pokemon.id)}</p>
          <h1>{formatName(pokemon.name)}</h1>
          {pokemon.category && <p className="detail__category">{pokemon.category}</p>}
          <TypeList types={pokemon.types} />
          <dl className="facts facts--row">
            <div><dt>Height</dt><dd>{formatMeters(pokemon.heightM)}</dd></div>
            <div><dt>Weight</dt><dd>{formatKg(pokemon.weightKg)}</dd></div>
            <div><dt>Last synced</dt><dd>{new Date(pokemon.syncedAt).toLocaleString()}</dd></div>
          </dl>
          <Link to={`/pokemon/${pokemon.name}`}>Full PokeAPI details →</Link>
        </div>
      </div>

      <section className="panel">
        <h2>My details</h2>
        {session ? (
          // key resets the form when a newer version arrives (after save, reload or conflict).
          <LocalPokemonForm key={`${pokemon.id}-${pokemon.version}`} pokemon={pokemon} onReload={() => refetch()} />
        ) : (
          <>
            <ReadOnlyDetails localizedName={pokemon.localizedName} region={pokemon.region} tags={pokemon.tags} notes={pokemon.notes} />
            <p className="muted"><Link to="/login" state={{ from: `/collection/${pokemon.id}` }}>Sign in</Link> to edit.</p>
          </>
        )}
      </section>

      {isAdmin && (
        <section className="panel panel--danger">
          <h2>Danger zone</h2>
          <p className="muted">Deleting removes the local copy and its custom fields. It can be synced again later.</p>
          <button type="button" className="btn btn--danger" onClick={handleDelete} disabled={remove.isPending}>
            {remove.isPending ? 'Deleting…' : 'Delete from My Pokedex'}
          </button>
          {remove.isError && <Notice kind="error">{remove.error.message}</Notice>}
        </section>
      )}
    </article>
  )
}

function ReadOnlyDetails({ localizedName, region, tags, notes }: { localizedName?: string; region?: string; tags: string[]; notes?: string }) {
  return (
    <dl className="facts">
      <div><dt>Localized name</dt><dd>{localizedName ?? '—'}</dd></div>
      <div><dt>Region</dt><dd>{region ?? '—'}</dd></div>
      <div><dt>Tags</dt><dd>{tags.length ? tags.join(', ') : '—'}</dd></div>
      <div><dt>Notes</dt><dd>{notes ?? '—'}</dd></div>
    </dl>
  )
}
