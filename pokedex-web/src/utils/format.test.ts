import { describe, expect, it } from 'vitest'
import { formatDexNumber, formatName, formatStatName } from './format'

describe('format', () => {
  it('formats PokeAPI slugs as display names', () => {
    expect(formatName('mr-mime')).toBe('Mr Mime')
    expect(formatName('pikachu')).toBe('Pikachu')
  })

  it('pads Pokedex numbers', () => {
    expect(formatDexNumber(25)).toBe('#0025')
    expect(formatDexNumber(1025)).toBe('#1025')
  })

  it('uses short labels for known stats', () => {
    expect(formatStatName('special-attack')).toBe('Sp. Atk')
    expect(formatStatName('hp')).toBe('HP')
    expect(formatStatName('accuracy')).toBe('Accuracy')
  })
})
