/**
 * Logical ranges of every measure typed in the field (professor's review,
 * point 1). They are the same the backend validators enforce, so the user sees
 * the error next to the field before saving.
 *
 * @author craulgomez
 */

export type Range = readonly [number, number]

export const LIMITS = {
  bagGrossWeight: [0.01, 60] as Range,
  bagTare: [0, 5] as Range,
  bagVolume: [0.005, 0.3] as Range,
  density: [10, 1500] as Range,
  containerVolume: [0.01, 1] as Range,
  containerWeight: [0.1, 500] as Range,
  students: 5000,
  teachers: 500,
  staff: 100,
} as const

function show(value: number): string {
  return value.toLocaleString('es-CO', { maximumFractionDigits: 3 })
}

/** Message when the value is outside the range, or undefined when it fits or is empty. */
export function rangeError(value: number | null, [min, max]: Range, label: string, unit: string): string | undefined {
  if (value === null) return undefined
  return value < min || value > max ? `${label} debe estar entre ${show(min)} y ${show(max)} ${unit}.` : undefined
}

/** Message when a count is above its maximum. */
export function countError(value: number, max: number, label: string): string | undefined {
  return value > max ? `${label}: máximo ${show(max)}.` : undefined
}
