import { createContext, useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { configureAuth } from '../api/client'
import { authApi } from '../api/pokedexApi'
import { isExpired, sessionFromToken, type Session } from './jwt'

const STORAGE_KEY = 'pokedex.token'

export interface AuthState {
  session: Session | null
  isAdmin: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthState | null>(null)

function readStoredSession(): Session | null {
  try {
    const token = sessionStorage.getItem(STORAGE_KEY)
    const session = token ? sessionFromToken(token) : null
    return session && !isExpired(session) ? session : null
  } catch {
    return null
  }
}

function storeToken(token: string | null): void {
  try {
    if (token) {
      sessionStorage.setItem(STORAGE_KEY, token)
    } else {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    return
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(readStoredSession)
  const sessionRef = useRef(session)
  sessionRef.current = session

  const logout = useCallback(() => {
    storeToken(null)
    setSession(null)
  }, [])

  useEffect(() => {
    configureAuth(() => sessionRef.current?.token ?? null, logout)
  }, [logout])

  useEffect(() => {
    if (!session) {
      return
    }
    const timer = window.setTimeout(logout, Math.max(0, session.expiresAt - Date.now()))
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const login = useCallback(async (username: string, password: string) => {
    const token = await authApi.token(username, password)
    const next = sessionFromToken(token)
    if (!next) {
      throw new Error('The server returned an unreadable token.')
    }
    storeToken(token)
    sessionRef.current = next
    setSession(next)
    await queryClient.invalidateQueries()
  }, [queryClient])

  const value = useMemo<AuthState>(() => ({
    session,
    isAdmin: session?.roles.includes('ADMIN') ?? false,
    login,
    logout,
  }), [session, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
