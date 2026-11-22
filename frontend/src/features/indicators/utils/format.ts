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
  BLANCO: '#ffffff',
  VERDE: '#2aa35f',
  NEGRO: '#25292a',
  SIN_COLOR: '#b98a3e',
}

export const SHIFT_LABELS: Record<ShiftTypeCode, string> = {
  MANANA: 'Mañana',
  TARDE: 'Tarde',
}
