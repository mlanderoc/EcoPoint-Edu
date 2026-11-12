/**
 * Field calculations shown before saving (Spec 004, R-302, R-343, R-344).
 * The backend calculates and stores the official values; these only help the
 * crew to catch mistakes while typing.
 *
 * @author craulgomez
 */

import { CharacterizationSettings, CharacterizationStudy, ShiftSlot, ShiftType } from '@/types'

/** Plausibility limits used when the institution has none (RN-037). */
export const DEFAULT_LIMITS: Pick<CharacterizationSettings, 'maxBagNetWeight' | 'minBagDensity' | 'maxBagDensity'> = {
  maxBagNetWeight: 50,
  minBagDensity: 20,
  maxBagDensity: 1000,
}

/**
 * Reads a number typed with a decimal comma or point (R-302).
 *
 * @returns the number, or null if the text is empty or not a number
 */
export function parseDecimal(text: string): number | null {
  const normalized = text.trim().replace(',', '.')
  if (normalized === '' || !/^\d*\.?\d+$|^\d+\.?$/.test(normalized)) {
    return null
  }
  return Number(normalized)
}

/** Net weight = gross − tare, with 2 decimals. */
export function netWeight(gross: number, tare: number): number {
  return Math.round((gross - tare) * 100) / 100
}

/** Density = net weight / volume, in kg/m³ with 2 decimals; null without volume. */
export function density(net: number, volume: number): number | null {
  return volume > 0 ? Math.round((net / volume) * 100) / 100 : null
}

/** True when the bag is outside the plausibility limits (RN-037): it is saved anyway, with a warning. */
export function isAtypical(net: number, bagDensity: number | null,
                           limits: Pick<CharacterizationSettings, 'maxBagNetWeight' | 'minBagDensity' | 'maxBagDensity'> = DEFAULT_LIMITS): boolean {
  return net > limits.maxBagNetWeight
    || (bagDensity !== null && (bagDensity < limits.minBagDensity || bagDensity > limits.maxBagDensity))
}

/** Formats kg or kg/m³ for people, with decimal comma. */
export function formatNumber(value: number | null | undefined, decimals = 2): string {
  if (value === null || value === undefined) {
    return '—'
  }
  return new Intl.NumberFormat('es-CO', { minimumFractionDigits: decimals, maximumFractionDigits: decimals }).format(value)
}

function localIsoDate(now: Date): string {
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/** Morning until noon, afternoon after (R-340). */
export function shiftTypeAt(now: Date): ShiftType {
  return now.getHours() < 12 ? 'MANANA' : 'TARDE'
}

/**
 * Suggests the shift to work on (R-340): today's shift if today is a day of the
 * study; otherwise an open shift; otherwise the first one not opened yet.
 */
export function suggestSlot(study: CharacterizationStudy, slots: ShiftSlot[], now: Date): { dayNumber: number; type: ShiftType } {
  const today = study.days.find((day) => day.date === localIsoDate(now))
  if (today) {
    return { dayNumber: today.dayNumber, type: shiftTypeAt(now) }
  }
  const candidate = slots.find((slot) => slot.status === 'ABIERTO')
    ?? slots.find((slot) => slot.status === null)
    ?? slots[slots.length - 1]
  return candidate ? { dayNumber: candidate.dayNumber, type: candidate.type } : { dayNumber: 1, type: 'MANANA' }
}

/** "1 bolsa", "3 bolsas". */
export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : pluralForm}`
}
