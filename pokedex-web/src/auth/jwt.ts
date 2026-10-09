export interface Session {
  token: string
  username: string
  roles: string[]
  expiresAt: number
}

export function sessionFromToken(token: string): Session | null {
  const parts = token.split('.')
  if (parts.length !== 3) {
    return null
  }
  try {
    const binary = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'))
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
    const claims = JSON.parse(new TextDecoder().decode(bytes)) as { sub?: string; roles?: unknown; exp?: number }
    if (typeof claims.sub !== 'string' || typeof claims.exp !== 'number') {
      return null
    }
    const roles = Array.isArray(claims.roles) ? claims.roles.filter((r): r is string => typeof r === 'string') : []
    return { token, username: claims.sub, roles, expiresAt: claims.exp * 1000 }
  } catch {
    return null
  }
}

export function isExpired(session: Session, now: number = Date.now()): boolean {
  return session.expiresAt <= now
}
