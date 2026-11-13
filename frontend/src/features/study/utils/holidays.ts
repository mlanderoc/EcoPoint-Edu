/**
 * Public holidays of Colombia, calculated for any year (spec 004, R-330; plan
 * D13, decision T1). Three kinds:
 * - fixed dates;
 * - dates moved to the following Monday by Law 51 of 1983 ("Ley Emiliani");
 * - dates that depend on Easter: Holy Thursday and Good Friday, and three
 *   more moved to Monday (Ascension, Corpus Christi and Sacred Heart).
 * Dates are ISO strings (YYYY-MM-DD) handled in UTC.
 *
 * @author craulgomez
 */

const DAY_MS = 24 * 60 * 60 * 1000
const MONDAY = 1

const FIXED: Array<[number, number, string]> = [
  [1, 1, 'Año Nuevo'],
  [5, 1, 'Día del Trabajo'],
  [7, 20, 'Día de la Independencia'],
  [8, 7, 'Batalla de Boyacá'],
  [12, 8, 'Inmaculada Concepción'],
  [12, 25, 'Navidad'],
]

const MOVED_TO_MONDAY: Array<[number, number, string]> = [
  [1, 6, 'Reyes Magos'],
  [3, 19, 'San José'],
  [6, 29, 'San Pedro y San Pablo'],
  [8, 15, 'Asunción de la Virgen'],
  [10, 12, 'Día de la Raza'],
  [11, 1, 'Todos los Santos'],
  [11, 11, 'Independencia de Cartagena'],
]

/** Days after Easter Sunday, and whether the holiday moves to Monday. */
const FROM_EASTER: Array<[number, boolean, string]> = [
  [-3, false, 'Jueves Santo'],
  [-2, false, 'Viernes Santo'],
  [39, true, 'Ascensión del Señor'],
  [60, true, 'Corpus Christi'],
  [68, true, 'Sagrado Corazón'],
]

const cache = new Map<number, Map<string, string>>()

function utc(year: number, month: number, day: number): Date {
  return new Date(Date.UTC(year, month - 1, day))
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * DAY_MS)
}

function nextMonday(date: Date): Date {
  return addDays(date, (7 + MONDAY - date.getUTCDay()) % 7)
}

/** Easter Sunday of a year (anonymous Gregorian algorithm). */
export function easterSunday(year: number): Date {
  const a = year % 19
  const b = Math.floor(year / 100)
  const c = year % 100
  const d = Math.floor(b / 4)
  const e = b % 4
  const f = Math.floor((b + 8) / 25)
  const g = Math.floor((b - f + 1) / 3)
  const h = (19 * a + b - d - g + 15) % 30
  const i = Math.floor(c / 4)
  const k = c % 4
  const l = (32 + 2 * e + 2 * i - h - k) % 7
  const m = Math.floor((a + 11 * h + 22 * l) / 451)
  const month = Math.floor((h + l - 7 * m + 114) / 31)
  const day = ((h + l - 7 * m + 114) % 31) + 1
  return utc(year, month, day)
}

/** Every holiday of a year: ISO date to its name. */
export function holidaysOf(year: number): Map<string, string> {
  const known = cache.get(year)
  if (known) return known
  const holidays = new Map<string, string>()
  FIXED.forEach(([month, day, name]) => holidays.set(toIso(utc(year, month, day)), name))
  MOVED_TO_MONDAY.forEach(([month, day, name]) => holidays.set(toIso(nextMonday(utc(year, month, day))), name))
  const easter = easterSunday(year)
  FROM_EASTER.forEach(([offset, moved, name]) => {
    const date = addDays(easter, offset)
    holidays.set(toIso(moved ? nextMonday(date) : date), name)
  })
  cache.set(year, holidays)
  return holidays
}

/** Name of the holiday of an ISO date, or null if it is not a holiday. */
export function holidayName(isoDate: string): string | null {
  return holidaysOf(Number(isoDate.slice(0, 4))).get(isoDate) ?? null
}
