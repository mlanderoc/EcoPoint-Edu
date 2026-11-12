/**
 * Dates of the 8 sampling days of a study (Spec 004, R-330, CA-301).
 * Dates travel as ISO strings (YYYY-MM-DD) and are handled in UTC so the time
 * zone of the browser never moves a day.
 *
 * @author craulgomez
 */

const DAY_MS = 24 * 60 * 60 * 1000
const SATURDAY = 6
const SUNDAY = 0

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

/**
 * Proposes days 2 to 8: the 7 business days (Monday to Friday) after day 1.
 * The teacher may change any of them for holidays or special school days.
 *
 * @param startDate date of day 1, YYYY-MM-DD
 * @returns the 7 following business days, YYYY-MM-DD
 */
export function calculateSamplingDays(startDate: string): string[] {
  const days: string[] = []
  let current = toUtc(startDate)
  while (days.length < 7) {
    current = new Date(current.getTime() + DAY_MS)
    const iso = toIso(current)
    if (!isWeekend(iso)) {
      days.push(iso)
    }
  }
  return days
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
