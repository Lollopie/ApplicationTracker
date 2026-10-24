import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { ApiError, isServerUnavailable, setUnauthorizedListener } from '../api/client'
import { queryKeys } from '../queryClient'
import { AuthContext, type AuthState } from './context'

const WAKE_UP_RETRIES = 25

async function fetchSession(): Promise<authApi.Me | null> {
  try {
    return await authApi.fetchMe()
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const { data: user, isError, failureReason } = useQuery({
    queryKey: queryKeys.me,
    queryFn: fetchSession,
    staleTime: Infinity,
    // A free-tier backend takes about a minute to wake up: keep trying for roughly two minutes.
    retry: (failureCount, error) => isServerUnavailable(error) && failureCount < WAKE_UP_RETRIES,
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 5000),
  })
  const waking = user === undefined && !isError && isServerUnavailable(failureReason)

  const endSession = useCallback(() => {
    // Drop everything cached for the previous user before switching the session state.
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== queryKeys.me[0] })
    queryClient.setQueryData(queryKeys.me, null)
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedListener(endSession)
    return () => setUnauthorizedListener(null)
  }, [endSession])

  const login = useCallback(
    async (credentials: authApi.Credentials) => {
      await authApi.login(credentials)
      const me = await authApi.fetchMe()
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== queryKeys.me[0] })
      queryClient.setQueryData(queryKeys.me, me)
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      // Even if the session already expired on the server, the client is logged out now.
      endSession()
    }
  }, [endSession])

  // If the session check itself failed (backend down), treat the user as logged out so the login page can show errors.
  const value = useMemo<AuthState>(
    () => ({ user: isError ? null : user, waking, login, logout }),
    [isError, user, waking, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
