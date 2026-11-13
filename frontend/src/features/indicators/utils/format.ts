/**
 * Number formats of the indicator screens: Colombian decimal comma and a dash
 * when the value could not be calculated (Spec 006, R-511, R-512).
 *
 * @author craulgomez
 */

import { ShiftTypeCode, WasteGroupCode } from '../types/indicatorTypes'

export function formatNumber(value: number | null | undefined, decimals = 2): string {
  if (value === null || value === undefined) return '—'
  return value.toLocaleString('es-CO', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

export const GROUP_COLORS: Record<WasteGroupCode, string> = {
  BLANCO: '#cbd5e1',
  VERDE: '#059669',
  NEGRO: '#1e293b',
  SIN_COLOR: '#d97706',
}

export const SHIFT_LABELS: Record<ShiftTypeCode, string> = {
  MANANA: 'Mañana',
  TARDE: 'Tarde',
}
