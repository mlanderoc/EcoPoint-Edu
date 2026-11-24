/**
 * Authentication API service.
 * Handles login, logout, and session verification via withCredentials HttpOnly cookies.
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { User } from '@/types'
import { LoginCredentials } from '../types'

export const authService = {
  /**
   * Submits credentials to authenticate the user and establish a secure session cookie.
   */
  login: async (credentials: LoginCredentials): Promise<User> => {
    return apiClient.post<User>('/api/v1/auth/login', credentials)
  },

  /**
   * Destroys the active session cookie on the backend.
   */
  logout: async (): Promise<void> => {
    return apiClient.post<void>('/api/v1/auth/logout')
  },

  /**
   * Sinks current session details from the active HttpOnly cookie.
   */
  getMe: async (): Promise<User> => {
    return apiClient.get<User>('/api/v1/auth/me')
  },

  /**
   * Asks for a link to create a new password (spec D18, RP-01). The answer is
   * the same whether the email has an account or not.
   */
  requestPasswordReset: async (email: string): Promise<void> => {
    return apiClient.post<void>('/api/v1/auth/password-reset/request', { email })
  },

  /**
   * Saves the new password with the code of the link (spec D18, RP-06).
   */
  confirmPasswordReset: async (token: string, newPassword: string): Promise<void> => {
    return apiClient.post<void>('/api/v1/auth/password-reset/confirm', { token, newPassword })
  },
}
