export function formatName(slug: string): string {
  return slug
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function formatDexNumber(id: number): string {
  return `#${String(id).padStart(4, '0')}`
}

const STAT_LABELS: Record<string, string> = {
  hp: 'HP',
  attack: 'Attack',
  defense: 'Defense',
  'special-attack': 'Sp. Atk',
  'special-defense': 'Sp. Def',
  speed: 'Speed',
}

export function formatStatName(name: string): string {
  return STAT_LABELS[name] ?? formatName(name)
}

export function formatKg(kg: number): string {
  return `${kg.toLocaleString(undefined, { maximumFractionDigits: 1 })} kg`
}

export function formatMeters(m: number): string {
  return `${m.toLocaleString(undefined, { maximumFractionDigits: 1 })} m`
}
