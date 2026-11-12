/**
 * Custom hook for accessing the authentication context.
 *
 * @author craulgomez
 */

import { useContext } from 'react'
import { AuthContext } from '../context/AuthContext'
import { AuthContextType } from '../types'

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

/**
 * Returns the authentication context, or null outside an AuthProvider.
 * Layout components use it so they can also render in isolation (stories, tests).
 */
export function useOptionalAuth(): AuthContextType | null {
  return useContext(AuthContext) ?? null
}
