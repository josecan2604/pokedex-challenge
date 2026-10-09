import { Link } from 'react-router-dom'
import type { EvolutionStage } from '../api/types'
import { formatName } from '../utils/format'

export function EvolutionChain({ chain, currentId }: { chain: EvolutionStage[]; currentId: number }) {
  if (chain.length <= 1) {
    return <p className="muted">This Pokémon does not evolve.</p>
  }
  const stages = groupByStage(chain)
  return (
    <ol className="evolution">
      {stages.map(([stage, members], index) => (
        <li key={stage} className="evolution__stage">
          {index > 0 && <span className="evolution__arrow" aria-hidden="true">→</span>}
          <ul className="evolution__members" aria-label={`Stage ${stage}`}>
            {members.map((member) => (
              <li key={member.id}>
                <Link
                  to={`/pokemon/${member.name}`}
                  className={`evolution__member${member.id === currentId ? ' is-current' : ''}`}
                  aria-current={member.id === currentId ? 'page' : undefined}
                >
                  <img src={member.spriteUrl} alt="" width={72} height={72} loading="lazy" />
                  <span>{formatName(member.name)}</span>
                </Link>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ol>
  )
}

function groupByStage(chain: EvolutionStage[]): [number, EvolutionStage[]][] {
  const groups = new Map<number, EvolutionStage[]>()
  for (const member of chain) {
    groups.set(member.stage, [...(groups.get(member.stage) ?? []), member])
  }
  return [...groups.entries()].sort(([a], [b]) => a - b)
}
