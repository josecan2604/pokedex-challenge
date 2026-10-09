import { describe, expect, it } from 'vitest'
import { parseTags, toCustomFields, validateCustomFields, validateRegistration } from './validation'

const valid = { localizedName: 'Pikachu', region: 'Kanto', tags: 'mascot', notes: '' }

describe('parseTags', () => {
  it('trims, lower-cases, drops blanks and de-duplicates', () => {
    expect(parseTags(' Starter, gen-1 ,, STARTER ')).toEqual(['starter', 'gen-1'])
  })

  it('returns an empty list for blank input', () => {
    expect(parseTags('   ')).toEqual([])
  })
})

describe('validateCustomFields', () => {
  it('accepts valid input', () => {
    expect(validateCustomFields(valid)).toEqual({})
  })

  it('mirrors the backend length limits', () => {
    const errors = validateCustomFields({
      localizedName: 'x'.repeat(101),
      region: 'y'.repeat(51),
      tags: '',
      notes: 'z'.repeat(501),
    })
    expect(Object.keys(errors).sort()).toEqual(['localizedName', 'notes', 'region'])
  })

  it('rejects tags with invalid characters', () => {
    expect(validateCustomFields({ ...valid, tags: 'ok, not ok!' }).tags).toContain('not ok!')
  })

  it('rejects more than ten tags', () => {
    const tags = Array.from({ length: 11 }, (_, i) => `t${i}`).join(',')
    expect(validateCustomFields({ ...valid, tags }).tags).toBe('At most 10 tags')
  })
})

describe('toCustomFields', () => {
  it('turns blank strings into null and parses tags', () => {
    expect(toCustomFields({ localizedName: '  ', region: ' Johto ', tags: 'A,b', notes: '' })).toEqual({
      localizedName: null,
      region: 'Johto',
      tags: ['a', 'b'],
      notes: null,
    })
  })
})

describe('validateRegistration', () => {
  it('accepts valid input', () => {
    expect(validateRegistration({ username: 'misty', email: 'misty@cerulean.com', password: 'Starmie123' })).toEqual({})
  })

  it('reports every invalid field', () => {
    const errors = validateRegistration({ username: 'a', email: 'nope', password: 'short' })
    expect(Object.keys(errors).sort()).toEqual(['email', 'password', 'username'])
  })

  it('requires a letter and a digit in the password', () => {
    expect(validateRegistration({ username: 'misty', email: 'm@c.com', password: 'onlyletters' }).password)
      .toMatch(/letter and one digit/)
  })
})
