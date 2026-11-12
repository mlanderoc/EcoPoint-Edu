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
}

/** Next step offered to the teacher: target status, button text and confirmation. */
export interface StatusStep {
  target: StudyStatus
  action: string
  confirmation: string
  needsReason: boolean
}

export const NEXT_STEP: Record<StudyStatus, StatusStep> = {
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
}

/** Name shown for a study, which has no name of its own (plan of Spec 003, R2). */
export function studyTitle(study: Pick<CharacterizationStudy, 'id' | 'startDate'>): string {
  return `Estudio N.º ${study.id} · ${formatDate(study.startDate)}`
}
