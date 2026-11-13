/**
 * Double entry, history and alerts routes of the REST API (Spec 003, section 5.3; Spec 005).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { PageResponse } from '@/types/api'
import { Alert, AlertType, CorrectionEntry, RecordType, VerifiableRecord, VerificationSummary } from '@/types'

export const verificationService = {
  summary: (studyId: number): Promise<VerificationSummary> =>
    apiClient.get<VerificationSummary>(`/api/v1/studies/${studyId}/verification-summary`),

  shiftRecords: (shiftId: number): Promise<VerifiableRecord[]> =>
    apiClient.get<VerifiableRecord[]>(`/api/v1/shifts/${shiftId}/records`),

  record: (type: RecordType, id: number): Promise<VerifiableRecord> =>
    apiClient.get<VerifiableRecord>(`/api/v1/records/${type}/${id}`),

  history: (type: RecordType, id: number): Promise<CorrectionEntry[]> =>
    apiClient.get<CorrectionEntry[]>(`/api/v1/records/${type}/${id}/history`),

  verify: (type: RecordType, id: number): Promise<unknown> =>
    apiClient.post(`/api/v1/verifications/${type}/${id}`),

  correct: (type: RecordType, id: number, values: Record<string, number>, reason: string): Promise<unknown> =>
    apiClient.post(`/api/v1/verifications/${type}/${id}/correct`, { values, reason }),
}

/** Filters of the alert tray (R-430). */
export interface AlertFilters {
  studyId?: number
  type?: AlertType
  attended?: boolean
}

export const alertService = {
  list: async (filters: AlertFilters = {}): Promise<Alert[]> =>
    (await apiClient.get<PageResponse<Alert>>('/api/v1/alerts', { params: { ...filters, size: 200 } })).items,

  /** Number of pending alerts for the menu counter (R-432). */
  pendingCount: async (): Promise<number> =>
    (await apiClient.get<PageResponse<Alert>>('/api/v1/alerts', { params: { attended: false, size: 1 } })).totalItems,

  attend: (id: number, reason: string): Promise<Alert> =>
    apiClient.post<Alert>(`/api/v1/alerts/${id}/attend`, { reason }),
}

/** Route kind of the record that caused an alert (R-430); null for a shift. */
export function recordTypeOf(targetType: string): RecordType | null {
  const types: Record<string, RecordType> = {
    'Población del turno': 'population', Bolsa: 'bag', Cuarteo: 'quartering', 'Medición de densidad': 'density',
  }
  return types[targetType] ?? null
}
