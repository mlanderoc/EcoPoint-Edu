/**
 * Domain model types matching Spec 001, Spec 003, and Spec 004.
 *
 * @author craulgomez
 */

export type UserRole = 'DOCENTE' | 'ESTUDIANTE_UNIVERSITARIO'

export type WasteGroup = 'BLANCO' | 'VERDE' | 'NEGRO' | 'SIN_COLOR'

export type SourceType =
  | 'AULAS'
  | 'OFICINAS'
  | 'BANOS'
  | 'CAFETERIA'
  | 'PATIO'
  | 'ZONAS_VERDES'
  | 'LABORATORIO'
  | 'OTRO'

export type ShiftType = 'MANANA' | 'TARDE'

export type ShiftStatus = 'NO_INICIADO' | 'ABIERTO' | 'CERRADO'

export type StudyStatus = 'PLANIFICADO' | 'EN_EJECUCION' | 'FINALIZADO' | 'CERRADO'

export type VerificationStatus = 'PENDIENTE' | 'VERIFICADO' | 'CORREGIDO' | 'RECHAZADO'

export interface User {
  id: number
  name: string
  lastName: string
  email: string
  role: UserRole
  active: boolean
  institutionId?: number
}

export interface Institution {
  id: number
  name: string
  address: string
  mapCenter?: {
    lat: number
    lng: number
  }
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
  teacherId: number
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
  name: string
  status: StudyStatus
  startDate: string
  endDate: string
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
