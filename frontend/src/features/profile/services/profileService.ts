/**
 * Profile of the user of the session (spec 003, R-132 and R-133; spec 004,
 * R-350 to R-352).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { UserProfile } from '@/types'

export const profileService = {
  /** Data, crews and own totals of records of the user of the session. */
  get: (): Promise<UserProfile> => apiClient.get<UserProfile>('/api/v1/auth/me/profile'),

  /** Changes the own password; the current one is checked by the API. */
  changePassword: (currentPassword: string, newPassword: string): Promise<void> =>
    apiClient.post<void>('/api/v1/auth/me/password', { currentPassword, newPassword }),
}
