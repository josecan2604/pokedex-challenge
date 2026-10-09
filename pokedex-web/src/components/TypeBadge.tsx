import { formatName } from '../utils/format'

export function TypeBadge({ type }: { type: string }) {
  return <span className={`type type--${type}`}>{formatName(type)}</span>
}

export function TypeList({ types }: { types: string[] }) {
  return (
    <ul className="type-list" aria-label="Types">
      {types.map((type) => (
        <li key={type}><TypeBadge type={type} /></li>
      ))}
    </ul>
  )
}
