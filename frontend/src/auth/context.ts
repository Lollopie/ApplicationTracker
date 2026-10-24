import { createContext, useContext } from 'react'
import type { Credentials, Me } from '../api/auth'

export interface AuthState {
  /** undefined while the session check is still running, null when logged out. */
  user: Me | null | undefined
  /** True while the session check is retrying because the backend is unavailable (starting up, if SERVER_SLEEPS). */
  waking: boolean
  login: (credentials: Credentials) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthState | null>(null)

export function useAuth(): AuthState {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('useAuth must be used inside <AuthProvider>')
  return auth
}
