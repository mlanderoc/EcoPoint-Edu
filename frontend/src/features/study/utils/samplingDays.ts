/**
 * Dates of the 8 sampling days of a study (spec 004, R-330, CA-301, CA-311).
 * A business day is Monday to Friday and not a public holiday of Colombia.
 * Dates travel as ISO strings (YYYY-MM-DD) and are handled in UTC so the time
 * zone of the browser never moves a day.
 *
 * @author craulgomez
 */

import { holidayName } from './holidays'

const DAY_MS = 24 * 60 * 60 * 1000
const SATURDAY = 6
const SUNDAY = 0
export const DAY_COUNT = 8

function toUtc(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day))
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10)
}

/** True if the ISO date is a Saturday or a Sunday. */
export function isWeekend(isoDate: string): boolean {
  const weekday = toUtc(isoDate).getUTCDay()
  return weekday === SATURDAY || weekday === SUNDAY
}

/** True if the ISO date is Monday to Friday and not a holiday. */
export function isBusinessDay(isoDate: string): boolean {
  return !isWeekend(isoDate) && holidayName(isoDate) === null
}

/**
 * Returns the business days that come after a date.
 *
 * @param afterDate date before the first one returned, YYYY-MM-DD
 * @param count     how many business days
 * @returns the business days, YYYY-MM-DD
 */
export function nextBusinessDays(afterDate: string, count: number): string[] {
  const days: string[] = []
  let current = toUtc(afterDate)
  while (days.length < count) {
    current = new Date(current.getTime() + DAY_MS)
    const iso = toIso(current)
    if (isBusinessDay(iso)) {
      days.push(iso)
    }
  }
  return days
}

/**
 * Proposes days 2 to 8: the 7 business days after day 1.
 *
 * @param startDate date of day 1, YYYY-MM-DD
 * @returns the 7 following business days, YYYY-MM-DD
 */
export function calculateSamplingDays(startDate: string): string[] {
  return nextBusinessDays(startDate, DAY_COUNT - 1)
}

/**
 * Changes the date of one day and recalculates every later day as the
 * following business days, even if some were changed by hand (R-330). Clearing
 * day 1 clears every day; clearing another day changes only that day.
 *
 * @param dates  the 8 dates, '' where a day has no date
 * @param index  position of the day that changed, 0 for day 1
 * @param value  new date of that day, YYYY-MM-DD, or ''
 * @returns the 8 dates after the change
 */
export function recalculateFrom(dates: string[], index: number, value: string): string[] {
  if (!value) {
    return index === 0 ? Array(DAY_COUNT).fill('') : dates.map((date, position) => (position === index ? '' : date))
  }
  const kept = [...dates.slice(0, index), value]
  return [...kept, ...nextBusinessDays(value, DAY_COUNT - kept.length)]
}

/** Why a date is not a business day, e.g. "festivo: Independencia de Cartagena", or null. */
export function nonBusinessReason(isoDate: string): string | null {
  const holiday = holidayName(isoDate)
  if (holiday) return `festivo: ${holiday}`
  return isWeekend(isoDate) ? 'fin de semana' : null
}

/** Formats an ISO date for people, e.g. "lun., 2 mar. 2026". */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) {
    return 'Sin fecha'
  }
  return new Intl.DateTimeFormat('es-CO', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(toUtc(isoDate))
}
