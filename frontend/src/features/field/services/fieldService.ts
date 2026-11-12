/**
 * Field registration routes of the REST API (Spec 003, section 5.2; Spec 004, phase 5).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { PageResponse } from '@/types/api'
import { SamplingShift, ShiftPopulation, ShiftType, WasteBag } from '@/types'

/** Weights of a bag as typed in the field. */
export interface BagInput {
  sourceId: number
  grossWeight: number
  tare: number
  volume: number
}

export const fieldService = {
  openShift: (studyId: number, dayNumber: number, type: ShiftType, crewId: number | null): Promise<SamplingShift> =>
    apiClient.post<SamplingShift>(`/api/v1/studies/${studyId}/shifts`, { dayNumber, type, crewId }),

  getShift: (shiftId: number): Promise<SamplingShift> =>
    apiClient.get<SamplingShift>(`/api/v1/shifts/${shiftId}`),

  registerPopulation: (shiftId: number, students: number, teachers: number, staff: number): Promise<ShiftPopulation> =>
    apiClient.put<ShiftPopulation>(`/api/v1/shifts/${shiftId}/population`, { students, teachers, staff }),

  listBags: async (shiftId: number): Promise<WasteBag[]> =>
    (await apiClient.get<PageResponse<WasteBag>>(`/api/v1/shifts/${shiftId}/bags`, { params: { size: 200 } })).items,

  registerBag: (shiftId: number, bag: BagInput): Promise<WasteBag> =>
    apiClient.post<WasteBag>(`/api/v1/shifts/${shiftId}/bags`, bag),

  setNoGeneration: (shiftId: number, sourceId: number, noGeneration: boolean): Promise<SamplingShift> =>
    noGeneration
      ? apiClient.post<SamplingShift>(`/api/v1/shifts/${shiftId}/sources/${sourceId}/no-generation`)
      : apiClient.delete<SamplingShift>(`/api/v1/shifts/${shiftId}/sources/${sourceId}/no-generation`),

  closeShift: (shiftId: number): Promise<SamplingShift> =>
    apiClient.post<SamplingShift>(`/api/v1/shifts/${shiftId}/close`),
}
