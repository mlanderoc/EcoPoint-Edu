/**
 * Live quality control of a quartering while it is typed (Spec 005, R-401,
 * R-404, R-405; RN-051 to RN-054). The backend decides the official status when
 * saving; this only shows it in advance.
 *
 * @author craulgomez
 */

import { CharacterizationSettings, QualityStatus, WasteGroup } from '@/types'

/** Settings used when the institution has none (RN-051, RN-052). */
export const DEFAULT_QUARTERING_SETTINGS: Pick<CharacterizationSettings,
  'minSampleWeight' | 'maxSampleWeight' | 'qualityTolerancePercent'> = {
  minSampleWeight: 45,
  maxSampleWeight: 55,
  qualityTolerancePercent: 2,
}

type QuarteringSettings = typeof DEFAULT_QUARTERING_SETTINGS

/** Live state of the control bar (R-404). */
export interface QualityCheck {
  classified: number
  differenceKg: number
  differencePercent: number
  status: QualityStatus
}

function round2(value: number): number {
  return Math.round(value * 100) / 100
}

/** Sum of the subcategory weights; empty ones count as 0 (R-402). */
export function classifiedTotal(weights: (number | null)[]): number {
  return round2(weights.reduce<number>((sum, weight) => sum + (weight ?? 0), 0))
}

/**
 * Compares the classified total with the subsample (RN-052): CONFORME when the
 * difference is at most the tolerance.
 */
export function checkQuality(sample: number, weights: (number | null)[],
                             settings: QuarteringSettings = DEFAULT_QUARTERING_SETTINGS): QualityCheck {
  const classified = classifiedTotal(weights)
  const differenceKg = round2(Math.abs(sample - classified))
  const differencePercent = sample > 0 ? round2((differenceKg * 100) / sample) : 100
  const conforming = sample > 0 && differenceKg * 100 <= sample * settings.qualityTolerancePercent + 1e-9
  return { classified, differenceKg, differencePercent, status: conforming ? 'CONFORME' : 'NO_CONFORME' }
}

/** Proposes a full sample when the shift weighs less than the minimum subsample (CA-401). */
export function proposedFullSample(shiftNet: number, settings: QuarteringSettings = DEFAULT_QUARTERING_SETTINGS): number | null {
  return shiftNet > 0 && shiftNet < settings.minSampleWeight ? round2(shiftNet) : null
}

/**
 * Checks the subsample weight against the range of the institution and the
 * net weight of the shift (RN-051, CA-400).
 *
 * @returns the message to show, or null when the subsample is valid
 */
export function sampleError(sample: number | null, shiftNet: number,
                            settings: QuarteringSettings = DEFAULT_QUARTERING_SETTINGS): string | null {
  if (sample === null || sample <= 0) {
    return 'Digite el peso de la submuestra.'
  }
  if (sample > shiftNet + 1e-9) {
    return `La submuestra no puede superar el peso neto del turno (${formatKg(shiftNet)} kg).`
  }
  const fullSample = proposedFullSample(shiftNet, settings)
  if (fullSample !== null) {
    return Math.abs(sample - fullSample) < 0.005
      ? null
      : `El turno generó ${formatKg(shiftNet)} kg, menos que la submuestra mínima: clasifique todo (muestra completa de ${formatKg(shiftNet)} kg).`
  }
  if (sample < settings.minSampleWeight || sample > settings.maxSampleWeight) {
    return `Debe estar entre ${formatKg(settings.minSampleWeight, 0)} y ${formatKg(settings.maxSampleWeight, 0)} kg`
  }
  return null
}

/** Weight of one subcategory with its group, to calculate the composition. */
export interface WeightedCategory {
  categoryId: number
  name: string
  group: WasteGroup
  weight: number
}

/** Composition by subcategory and by group over the classified total (RN-053, RN-054, CA-406). */
export function composition(items: WeightedCategory[]): {
  byCategory: (WeightedCategory & { percent: number })[]
  byGroup: Record<WasteGroup, number>
} {
  const total = items.reduce((sum, item) => sum + item.weight, 0)
  const percent = (weight: number) => (total > 0 ? round2((weight * 100) / total) : 0)
  const byGroup: Record<WasteGroup, number> = { BLANCO: 0, VERDE: 0, NEGRO: 0, SIN_COLOR: 0 }
  for (const group of Object.keys(byGroup) as WasteGroup[]) {
    byGroup[group] = percent(items.filter((item) => item.group === group).reduce((sum, item) => sum + item.weight, 0))
  }
  return { byCategory: items.map((item) => ({ ...item, percent: percent(item.weight) })), byGroup }
}

/** Formats kg with decimal comma. */
export function formatKg(value: number, decimals = 2): string {
  return new Intl.NumberFormat('es-CO', { minimumFractionDigits: decimals, maximumFractionDigits: decimals }).format(value)
}
