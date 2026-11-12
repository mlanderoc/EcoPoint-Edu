/**
 * Studies of the REST API (Spec 003, section 5.2; Spec 004, R-330 to R-333).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { CharacterizationStudy, ShiftSlot, StudyStatus } from '@/types'

/** Date of one sampling day; null while it is not scheduled. */
export interface StudyDayInput {
  dayNumber: number
  date: string | null
}

export const studyService = {
  list: (institutionId?: number): Promise<CharacterizationStudy[]> =>
    apiClient.get<CharacterizationStudy[]>('/api/v1/studies', { params: { institutionId } }),

  get: (id: number): Promise<CharacterizationStudy> =>
    apiClient.get<CharacterizationStudy>(`/api/v1/studies/${id}`),

  create: (institutionId: number | undefined, days: StudyDayInput[]): Promise<CharacterizationStudy> =>
    apiClient.post<CharacterizationStudy>('/api/v1/studies', { institutionId, days }),

  setSources: (id: number, sourceIds: number[]): Promise<CharacterizationStudy> =>
    apiClient.put<CharacterizationStudy>(`/api/v1/studies/${id}/sources`, { sourceIds }),

  changeStatus: (id: number, status: StudyStatus, reason?: string): Promise<CharacterizationStudy> =>
    apiClient.post<CharacterizationStudy>(`/api/v1/studies/${id}/status`, { status, reason }),

  shifts: (id: number): Promise<ShiftSlot[]> =>
    apiClient.get<ShiftSlot[]>(`/api/v1/studies/${id}/shifts`),
}
