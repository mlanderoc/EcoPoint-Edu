/**
 * Domain types as the REST API of Spec 003 returns them (DTOs of com.ecopoint.dto).
 * Enum values mirror the Java model of Spec 001.
 *
 * @author craulgomez
 */

export type UserRole = 'DOCENTE' | 'ESTUDIANTE_UNIVERSITARIO'

export type WasteGroup = 'BLANCO' | 'VERDE' | 'NEGRO' | 'SIN_COLOR'

export type SourceType =
  | 'AULAS'
  | 'OFICINAS'
  | 'TIENDA_ESCOLAR'
  | 'PAE'
  | 'ZONAS_VERDES'
  | 'BANOS'
  | 'LABORATORIO'
  | 'OTRA'

export type ShiftType = 'MANANA' | 'TARDE'

export type ShiftStatus = 'ABIERTO' | 'CERRADO'

export type StudyStatus = 'PLANIFICADO' | 'EN_EJECUCION' | 'EN_DEPURACION' | 'CERRADO'

export type VerificationStatus = 'PENDIENTE' | 'VERIFICADO' | 'CORREGIDO'

export type QualityStatus = 'CONFORME' | 'NO_CONFORME' | 'ANULADO'

export type AlertType = 'CUARTEO_NO_CONFORME' | 'VALOR_ATIPICO' | 'VERIFICACION_PENDIENTE'

/** State of a participating source in a shift (RN-024, R-345). */
export type SourceShiftStatus = 'CON_GENERACION' | 'SIN_GENERACION' | 'PENDIENTE'

/** UserResponse. `permissions` and `institutionName` come with login and /auth/me. */
export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  active: boolean
  institutionId?: number
  institutionName?: string | null
  permissions?: string[]
}

export interface GeoPoint {
  latitude: number
  longitude: number
}

export interface CharacterizationSettings {
  minSampleWeight: number
  maxSampleWeight: number
  qualityTolerancePercent: number
  maxBagNetWeight: number
  minBagDensity: number
  maxBagDensity: number
}

export interface Institution {
  id: number
  name: string
  address: string
  mapCenter?: GeoPoint | null
  mapZoom?: number
  settings?: CharacterizationSettings
}

export interface GenerationSource {
  id: number
  institutionId: number
  code: string
  name: string
  type: SourceType
  active: boolean
  hasGeometry?: boolean
}

export interface Crew {
  id: number
  name: string
  responsibleId: number | null
  responsibleName?: string | null
  memberIds: number[]
}

export interface WasteCategory {
  id: number
  name: string
  group: WasteGroup
  description?: string
  active: boolean
  hazardous?: boolean
}

export interface SamplingDay {
  dayNumber: number
  date: string | null
  /** Day 1 is discarded: it does not enter the calculations (R-333). */
  discarded: boolean
}

export interface CharacterizationStudy {
  id: number
  institutionId: number
  status: StudyStatus
  startDate: string | null
  endDate: string | null
  days: SamplingDay[]
  participatingSourceIds: number[]
  pendingRecords: number
}

export interface Verification {
  status: VerificationStatus
  enteredBy: number | null
  enteredAt: string | null
  verifiedBy: number | null
  verifiedAt: string | null
}

export interface ShiftPopulation {
  id?: number | null
  students: number
  teachers: number
  staff: number
  total: number
  verification?: Verification | null
}

export interface SourceProgress {
  sourceId: number
  code: string
  name: string
  type: SourceType
  bagCount: number
  totalNetWeight: number
  status: SourceShiftStatus
}

export interface SamplingShift {
  id: number
  studyId: number
  dayNumber: number
  type: ShiftType
  code: string
  status: ShiftStatus
  crewId: number | null
  population: ShiftPopulation | null
  bagCount: number
  totalNetWeight: number
  sourcesWithoutGeneration: number[]
  hasQuartering: boolean
  hasDensity: boolean
  sources: SourceProgress[]
}

/** One of the 16 possible shifts of a study, opened or not (status null). */
export interface ShiftSlot {
  dayNumber: number
  type: ShiftType
  shiftId: number | null
  status: ShiftStatus | null
  coveredSources: number
  totalSources: number
}

export interface WasteBag {
  id: number
  shiftId: number
  sourceId: number
  sourceCode: string
  label: string
  grossWeight: number
  tare: number
  netWeight: number
  volume: number
  density: number | null
  atypical: boolean
  verification: Verification
}
