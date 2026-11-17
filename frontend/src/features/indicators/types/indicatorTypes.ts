/**
 * Baseline indicators as GET /api/v1/studies/{id}/indicators returns them
 * (Spec 006, T501). Values arrive already rounded (R-511); null means the
 * data needed to calculate the value is missing (R-512).
 *
 * @author craulgomez
 */

import type { StudyStatus } from '@/types'

export type WasteGroupCode = 'BLANCO' | 'VERDE' | 'NEGRO' | 'SIN_COLOR'
export type ShiftTypeCode = 'MANANA' | 'TARDE'

export interface SummaryKPIs {
  averageDailyGeneration: number | null
  averagePpc: number | null
  ppcStandardDeviation: number | null
  ppcVariationCoefficient: number | null
  averageDensity: number | null
  recoverableFraction: number | null
  recoverableDailyGeneration: number | null
  countedDays: number
}

export interface DailyGeneration {
  dayNumber: number
  date: string | null
  excluded: boolean
  morningGeneration: number
  afternoonGeneration: number
  generation: number
  population: number
  ppc: number | null
}

export interface SourceGeneration {
  sourceId: number
  code: string
  name: string
  averageDailyGeneration: number
}

export interface WasteGroupComposition {
  group: WasteGroupCode
  name: string
  description: string
  weight: number
  percentage: number | null
}

export interface WasteSubcategoryComposition {
  categoryId: number
  name: string
  group: WasteGroupCode
  weight: number
  percentage: number | null
}

export interface Composition {
  groups: WasteGroupComposition[]
  subcategories: WasteSubcategoryComposition[]
  shiftsWithQuartering: number
  expectedShifts: number
  note: string
}

export interface ShiftComparison {
  type: ShiftTypeCode
  averageGeneration: number | null
  averagePopulation: number | null
  averagePpc: number | null
  averageDensity: number | null
  composition: Composition
}

export interface DataQuality {
  totalRecords: number
  verifiedRecords: number
  verifiedPercentage: number | null
  correctedRecords: number
  nonConformingQuarterings: number
}

export interface StudyIndicators {
  studyId: number
  status: StudyStatus
  official: boolean
  label: 'Oficial' | 'Preliminar'
  summary: SummaryKPIs
  days: DailyGeneration[]
  sources: SourceGeneration[]
  shifts: ShiftComparison[]
  composition: Composition
  quality: DataQuality
}
