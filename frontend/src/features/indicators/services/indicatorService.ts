/**
 * Indicator routes of the REST API (Spec 006, T504 and T506).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { StudyIndicators } from '../types/indicatorTypes'

export const indicatorService = {
  get: (studyId: number) => apiClient.get<StudyIndicators>(`/api/v1/studies/${studyId}/indicators`),
}
