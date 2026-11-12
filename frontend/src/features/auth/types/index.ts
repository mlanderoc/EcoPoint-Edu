/**
 * Types and interfaces for the authentication feature module.
 *
 * @author craulgomez
 */

import { User, UserRole } from '@/types'

export interface LoginCredentials {
  email: string
  password: string
}

export interface AuthContextType {
  user: User | null
  role: UserRole | null
  isLoading: boolean
  isAuthenticated: boolean
  sessionExpired: boolean
  login: (credentials: LoginCredentials) => Promise<User>
  logout: () => Promise<void>
  clearSessionExpired: () => void
  refreshUser: () => Promise<User | null>
}
