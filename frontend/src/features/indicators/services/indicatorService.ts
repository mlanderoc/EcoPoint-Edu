/**
 * Indicator and report routes of the REST API (Spec 006, T504, T506 and T512).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { StudyIndicators } from '../types/indicatorTypes'

/** Report formats the API can export (R-530, R-531). */
export type ReportFormat = 'excel' | 'pdf'

/** Name of the downloaded file, the same the API sends. */
export function reportFileName(studyId: number, format: ReportFormat): string {
  return format === 'excel'
    ? `ecopoint-reporte-estudio-${studyId}.xlsx`
    : `ecopoint-informe-linea-base-${studyId}.pdf`
}

export const indicatorService = {
  get: (studyId: number) => apiClient.get<StudyIndicators>(`/api/v1/studies/${studyId}/indicators`),

  /** Excel workbook or PDF report of a study. */
  report: (studyId: number, format: ReportFormat) =>
    apiClient.getBlob(`/api/v1/studies/${studyId}/reports/${format}`),
}
