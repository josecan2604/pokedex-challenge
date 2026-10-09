import type { PokemonStat } from '../api/types'
import { formatStatName } from '../utils/format'

const MAX_BASE_STAT = 255

export function StatBars({ stats }: { stats: PokemonStat[] }) {
  const total = stats.reduce((sum, stat) => sum + stat.baseValue, 0)
  return (
    <table className="stats">
      <caption className="visually-hidden">Base stats</caption>
      <tbody>
        {stats.map((stat) => (
          <tr key={stat.name}>
            <th scope="row">{formatStatName(stat.name)}</th>
            <td className="stats__value">{stat.baseValue}</td>
            <td className="stats__bar-cell">
              <div
                className="stats__bar"
                role="meter"
                aria-label={formatStatName(stat.name)}
                aria-valuemin={0}
                aria-valuemax={MAX_BASE_STAT}
                aria-valuenow={stat.baseValue}
              >
                <span style={{ width: `${Math.min(100, (stat.baseValue / MAX_BASE_STAT) * 100)}%` }} />
              </div>
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <th scope="row">Total</th>
          <td className="stats__value">{total}</td>
          <td />
        </tr>
      </tfoot>
    </table>
  )
}
