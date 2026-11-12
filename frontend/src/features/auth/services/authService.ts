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
}
