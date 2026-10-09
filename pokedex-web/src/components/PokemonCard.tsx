import { Link } from 'react-router-dom'
import type { PokemonSummary } from '../api/types'
import { formatDexNumber, formatKg, formatName } from '../utils/format'
import { TypeList } from './TypeBadge'

export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  return (
    <article className="card">
      <Link to={`/pokemon/${pokemon.name}`} className="card__link" aria-label={`${formatName(pokemon.name)} details`}>
        <div className="card__media">
          <img src={pokemon.spriteUrl} alt="" width={96} height={96} loading="lazy" />
        </div>
        <div className="card__body">
          <p className="card__number">{formatDexNumber(pokemon.id)}</p>
          <h2 className="card__title">{formatName(pokemon.name)}</h2>
          {pokemon.category && <p className="card__subtitle">{pokemon.category}</p>}
          <TypeList types={pokemon.types} />
          <dl className="facts">
            <div>
              <dt>Weight</dt>
              <dd>{formatKg(pokemon.weightKg)}</dd>
            </div>
            <div>
              <dt>Abilities</dt>
              <dd>{pokemon.abilities.map(formatName).join(', ')}</dd>
            </div>
          </dl>
        </div>
      </Link>
    </article>
  )
}
