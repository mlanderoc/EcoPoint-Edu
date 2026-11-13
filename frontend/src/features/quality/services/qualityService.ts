/**
 * Quartering and density routes of the REST API (Spec 003, section 5.3; Spec 005).
 *
 * @author craulgomez
 */

import { ApiError, apiClient } from '@/services/apiClient'
import { DensityMeasurement, Quartering } from '@/types'

/** Weight of one subcategory in a new quartering. */
export interface QuarteringResultInput {
  categoryId: number
  weight: number
}

async function orNull<T>(request: Promise<T>): Promise<T | null> {
  try {
    return await request
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) {
      return null
    }
    throw err
  }
}

export const qualityService = {
  /** Quartering of the shift, or null if it was not registered yet. */
  getQuartering: (shiftId: number): Promise<Quartering | null> =>
    orNull(apiClient.get<Quartering>(`/api/v1/shifts/${shiftId}/quartering`)),

  registerQuartering: (shiftId: number, sampleWeight: number, results: QuarteringResultInput[]): Promise<Quartering> =>
    apiClient.post<Quartering>(`/api/v1/shifts/${shiftId}/quartering`, { sampleWeight, results }),

  annulQuartering: (shiftId: number, reason: string): Promise<Quartering> =>
    apiClient.post<Quartering>(`/api/v1/shifts/${shiftId}/quartering/annul`, { reason }),

  /** Density of the shift, or null if it was not measured yet. */
  getDensity: (shiftId: number): Promise<DensityMeasurement | null> =>
    orNull(apiClient.get<DensityMeasurement>(`/api/v1/shifts/${shiftId}/density`)),

  registerDensity: (shiftId: number, containerVolume: number, netWeight: number): Promise<DensityMeasurement> =>
    apiClient.post<DensityMeasurement>(`/api/v1/shifts/${shiftId}/density`, { containerVolume, netWeight }),
}
