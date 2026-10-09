import { describe, expect, it } from 'vitest'
import { isExpired, sessionFromToken } from './jwt'

function fakeToken(claims: object): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `${encode({ alg: 'RS256' })}.${encode(claims)}.signature`
}

describe('sessionFromToken', () => {
  it('reads subject, roles and expiry', () => {
    const session = sessionFromToken(fakeToken({ sub: 'ash', roles: ['USER'], exp: 2_000_000_000 }))

    expect(session).toMatchObject({ username: 'ash', roles: ['USER'], expiresAt: 2_000_000_000_000 })
  })

  it('handles non-ASCII claims', () => {
    const session = sessionFromToken(fakeToken({ sub: 'ash', exp: 1, name: 'Pokémon' }))

    expect(session?.username).toBe('ash')
  })

  it('treats a missing roles claim as no roles', () => {
    expect(sessionFromToken(fakeToken({ sub: 'ash', exp: 1 }))?.roles).toEqual([])
  })

  it.each(['', 'not-a-jwt', 'a.b', 'a.###.c'])('rejects malformed token %j', (token) => {
    expect(sessionFromToken(token)).toBeNull()
  })

  it('rejects tokens without sub or exp', () => {
    expect(sessionFromToken(fakeToken({ roles: ['USER'] }))).toBeNull()
  })
})

describe('isExpired', () => {
  it('compares expiry with now', () => {
    const session = { token: 't', username: 'ash', roles: [], expiresAt: 1_000 }
    expect(isExpired(session, 999)).toBe(false)
    expect(isExpired(session, 1_000)).toBe(true)
  })
})
