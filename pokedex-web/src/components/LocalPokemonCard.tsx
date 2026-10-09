import { Link } from 'react-router-dom'
import type { LocalPokemon } from '../api/types'
import { formatDexNumber, formatName } from '../utils/format'
import { TypeList } from './TypeBadge'

export function LocalPokemonCard({ pokemon }: { pokemon: LocalPokemon }) {
  const displayName = formatName(pokemon.name)
  return (
    <article className="card">
      <Link to={`/collection/${pokemon.id}`} className="card__link" aria-label={`${displayName} in my Pokedex`}>
        <div className="card__media">
          {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt="" width={96} height={96} loading="lazy" />}
        </div>
        <div className="card__body">
          <p className="card__number">{formatDexNumber(pokemon.id)}</p>
          <h2 className="card__title">{displayName}</h2>
          {pokemon.localizedName && pokemon.localizedName.toLowerCase() !== pokemon.name && (
            <p className="card__subtitle">aka {pokemon.localizedName}</p>
          )}
          <TypeList types={pokemon.types} />
          {pokemon.region && <p className="card__meta">📍 {pokemon.region}</p>}
          {pokemon.tags.length > 0 && (
            <ul className="tag-list" aria-label="Tags">
              {pokemon.tags.map((tag) => <li key={tag} className="tag">{tag}</li>)}
            </ul>
          )}
        </div>
      </Link>
    </article>
  )
}
