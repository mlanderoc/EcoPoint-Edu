/**
 * Authentication Context and Provider for session synchronization (T309 / R-310 / R-312).
 *
 * @author craulgomez
 */

import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import { onUnauthorized } from '@/services/apiClient'
import { User, UserRole } from '@/types'
import { authService } from '../services/authService'
import { AuthContextType, LoginCredentials } from '../types'

export const AuthContext = createContext<AuthContextType | undefined>(undefined)

export interface AuthProviderProps {
  children: React.ReactNode
  initialUser?: User | null
  skipInitialFetch?: boolean
}

export function AuthProvider({
  children,
  initialUser = null,
  skipInitialFetch = false,
}: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(initialUser)
  const [isLoading, setIsLoading] = useState<boolean>(!skipInitialFetch && initialUser === null)
  const [sessionExpired, setSessionExpired] = useState<boolean>(false)

  // Listen for global HTTP 401 errors from apiClient
  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      setUser(null)
      setSessionExpired(true)
    })
    return unsubscribe
  }, [])

  // Sync user from backend cookie on initial mount if not skipped
  const refreshUser = useCallback(async (): Promise<User | null> => {
    try {
      setIsLoading(true)
      const currentUser = await authService.getMe()
      setUser(currentUser)
      setSessionExpired(false)
      return currentUser
    } catch {
      setUser(null)
      return null
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    if (!skipInitialFetch && initialUser === null) {
      refreshUser()
    }
  }, [skipInitialFetch, initialUser, refreshUser])

  const login = useCallback(
    async (credentials: LoginCredentials): Promise<User> => {
      setIsLoading(true)
      try {
        // Step 1: Submit login credentials
        await authService.login(credentials)
        // Step 2: Strictly synchronize with GET /api/v1/auth/me (T309)
        const me = await authService.getMe()
        setUser(me)
        setSessionExpired(false)
        return me
      } finally {
        setIsLoading(false)
      }
    },
    []
  )

  const logout = useCallback(async (): Promise<void> => {
    setIsLoading(true)
    try {
      await authService.logout()
    } catch (err) {
      console.warn('Logout request encountered an error, clearing client state:', err)
    } finally {
      setUser(null)
      setSessionExpired(false)
      setIsLoading(false)
    }
  }, [])

  const clearSessionExpired = useCallback(() => {
    setSessionExpired(false)
  }, [])

  const role: UserRole | null = user?.role ?? null
  const isAuthenticated: boolean = Boolean(user)

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      role,
      isLoading,
      isAuthenticated,
      sessionExpired,
      login,
      logout,
      clearSessionExpired,
      refreshUser,
    }),
    [user, role, isLoading, isAuthenticated, sessionExpired, login, logout, clearSessionExpired, refreshUser]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
