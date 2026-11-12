/**
 * Domain model types matching Spec 001, Spec 002, and Spec 003.
 * Mirroring Java backend models in com.ecopoint.model.
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

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  active: boolean
  institutionId?: number
}

export interface GeoPoint {
  latitude: number
  longitude: number
}

export interface Institution {
  id: number
  name: string
  address: string
  mapCenter?: GeoPoint
  mapZoom?: number
}

export interface GenerationSource {
  id: number
  institutionId: number
  code: string
  name: string
  type: SourceType
  active: boolean
}

export interface Crew {
  id: number
  name: string
  responsibleId: number
  memberIds: number[]
}

export interface WasteCategory {
  id: number
  name: string
  group: WasteGroup
  active: boolean
}

export interface SamplingDay {
  dayNumber: number
  date: string
  excluded: boolean
}

export interface CharacterizationStudy {
  id: number
  institutionId: number
  name?: string
  status: StudyStatus
  startDate?: string
  endDate?: string
  days: SamplingDay[]
  participatingSourceIds: number[]
}

export interface ShiftPopulation {
  students: number
  teachers: number
  staff: number
  total: number
}

export interface SamplingShift {
  id: number
  studyId: number
  dayNumber: number
  type: ShiftType
  status: ShiftStatus
  crewId?: number
  population?: ShiftPopulation
  sourcesWithoutGeneration: number[]
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
  density: number
  photoUrl?: string
  verificationStatus: VerificationStatus
}

export interface SourceShiftSummary {
  source: GenerationSource
  bagCount: number
  totalNetWeight: number
  status: 'CON_GENERACION' | 'SIN_GENERACION' | 'PENDIENTE'
}
