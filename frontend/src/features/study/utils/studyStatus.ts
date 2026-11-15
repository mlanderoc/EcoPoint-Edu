/**
 * Labels and next step of the study lifecycle (RN-014 to RN-017, R-332).
 *
 * @author craulgomez
 */

import { CharacterizationStudy, StudyStatus } from '@/types'
import { formatDate } from './samplingDays'

export const STATUS_LABELS: Record<StudyStatus, string> = {
  PLANIFICADO: 'Planificado',
  EN_EJECUCION: 'En ejecución',
  EN_DEPURACION: 'En depuración',
  CERRADO: 'Cerrado',
  CANCELADO: 'Cancelado',
}

/** Next step offered to the teacher: target status, button text and confirmation. */
export interface StatusStep {
  target: StudyStatus
  action: string
  confirmation: string
  needsReason: boolean
}

/** Next step of each status; a cancelled study has none, because CANCELADO is final (RN-018). */
export const NEXT_STEP: Record<StudyStatus, StatusStep | null> = {
  PLANIFICADO: {
    target: 'EN_EJECUCION',
    action: 'Iniciar ejecución',
    confirmation: 'El estudio pasará a ejecución y las cuadrillas podrán registrar datos en campo.',
    needsReason: false,
  },
  EN_EJECUCION: {
    target: 'EN_DEPURACION',
    action: 'Pasar a depuración',
    confirmation: 'Terminará el registro en campo; desde ahora solo se verifican y corrigen los datos.',
    needsReason: false,
  },
  EN_DEPURACION: {
    target: 'CERRADO',
    action: 'Cerrar estudio',
    confirmation: 'Los indicadores quedarán oficiales. Solo se puede cerrar sin registros pendientes de verificación.',
    needsReason: false,
  },
  CERRADO: {
    target: 'EN_DEPURACION',
    action: 'Reabrir para depuración',
    confirmation: 'El estudio volverá a depuración. Escriba el motivo: queda registrado con su nombre y la fecha.',
    needsReason: true,
  },
  CANCELADO: null,
}

/** A planned study can still be deleted (RN-019). */
export function canDelete(status: StudyStatus): boolean {
  return status === 'PLANIFICADO'
}

/** A study being executed or cleaned can be cancelled (RN-018). */
export function canCancel(status: StudyStatus): boolean {
  return status === 'EN_EJECUCION' || status === 'EN_DEPURACION'
}

/** Name shown for a study, which has no name of its own (plan of Spec 003, R2). */
export function studyTitle(study: Pick<CharacterizationStudy, 'id' | 'startDate'>): string {
  return `Estudio N.º ${study.id} · ${formatDate(study.startDate)}`
}
