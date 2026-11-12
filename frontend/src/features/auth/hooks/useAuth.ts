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
