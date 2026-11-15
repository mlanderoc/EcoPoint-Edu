/**
 * Lifecycle of a study: progress of its 8 days, the step to the next status,
 * the dates of a planned study, and deleting or cancelling it (Spec 004, T316;
 * R-332 to R-336, CA-309, CA-315).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Ban, CalendarDays, Trash2 } from 'lucide-react'
import { AlertBanner, BackButton, Button, Card, CardContent, CardHeader, CardTitle, Input, Modal } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, ShiftSlot } from '@/types'
import { cn } from '@/utils/cn'
import { StudyDatesEditor } from '../components/StudyDatesEditor'
import { studyService } from '../services/studyService'
import { formatDate } from '../utils/samplingDays'
import { NEXT_STEP, STATUS_LABELS, canCancel, canDelete, studyTitle } from '../utils/studyStatus'

type Dialog = 'status' | 'cancel' | 'delete' | null

function slotLabel(slot: ShiftSlot | undefined): string {
  if (!slot || slot.status === null) return 'Sin abrir'
  return slot.status === 'CERRADO' ? 'Cerrado' : `Abierto ${slot.coveredSources}/${slot.totalSources}`
}

function slotStyle(slot: ShiftSlot | undefined): string {
  if (!slot || slot.status === null) return 'bg-slate-50 text-slate-500 border-slate-200'
  return slot.status === 'CERRADO'
    ? 'bg-[var(--color-primary-light)] text-[var(--color-primary-hover)] border-emerald-200'
    : 'bg-amber-50 text-amber-900 border-amber-200'
}

function datesOf(study: CharacterizationStudy): string[] {
  return study.days.map((day) => day.date ?? '')
}

export function StudyDetailPage() {
  const { id } = useParams()
  const studyId = Number(id)
  const navigate = useNavigate()
  const [study, setStudy] = useState<CharacterizationStudy | null>(null)
  const [slots, setSlots] = useState<ShiftSlot[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [rejection, setRejection] = useState<string | null>(null)
  const [dialog, setDialog] = useState<Dialog>(null)
  const [reason, setReason] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [dates, setDates] = useState<string[] | null>(null)
  const [datesSaved, setDatesSaved] = useState(false)

  const load = useCallback(async () => {
    try {
      const [loaded, loadedSlots] = await Promise.all([studyService.get(studyId), studyService.shifts(studyId)])
      setStudy(loaded)
      setSlots(loadedSlots)
    } catch (err) {
      setLoadError(err instanceof ApiError ? err.message : 'No se pudo cargar el estudio.')
    }
  }, [studyId])

  useEffect(() => {
    load()
  }, [load])

  if (loadError) {
    return <AlertBanner variant="danger" title="Estudio no disponible" message={loadError} />
  }
  if (!study) {
    return <p className="text-sm text-[var(--color-text-muted)]">Cargando estudio...</p>
  }

  const step = NEXT_STEP[study.status]
  const editedDates = dates ?? datesOf(study)
  const datesChanged = editedDates.some((date, index) => date !== datesOf(study)[index])

  const closeDialog = () => {
    setDialog(null)
    setReason('')
  }

  const run = async (action: () => Promise<void>, failure: string) => {
    setRejection(null)
    try {
      setIsSaving(true)
      await action()
    } catch (err) {
      setRejection(err instanceof ApiError ? err.message : failure)
    } finally {
      setIsSaving(false)
      closeDialog()
    }
  }

  const handleStatus = () => run(async () => {
    if (!step) return
    setStudy(await studyService.changeStatus(study.id, step.target, step.needsReason ? reason : undefined))
  }, 'No se pudo cambiar el estado del estudio.')

  const handleCancel = () => run(async () => {
    setStudy(await studyService.cancel(study.id, reason.trim()))
  }, 'No se pudo cancelar el estudio.')

  const handleDelete = () => run(async () => {
    await studyService.remove(study.id)
    navigate('/estudios')
  }, 'No se pudo eliminar el estudio.')

  const handleSaveDates = () => run(async () => {
    const updated = await studyService.reschedule(study.id,
      editedDates.map((date, index) => ({ dayNumber: index + 1, date: date || null })))
    setStudy(updated)
    setDates(null)
    setDatesSaved(true)
  }, 'No se pudieron guardar las fechas.')

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      <BackButton to="/estudios" label="Atrás a los estudios" />
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-[var(--color-brand-dark)]">{studyTitle(study)}</h1>
          <p className="text-sm text-[var(--color-text-muted)]">
            Estado: <strong>{STATUS_LABELS[study.status]}</strong> · Registros pendientes de verificación:{' '}
            <strong>{study.pendingRecords}</strong>
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {canCancel(study.status) && (
            <Button variant="outline" className="min-h-[48px] text-[var(--color-danger)]"
              onClick={() => setDialog('cancel')}>
              <Ban className="w-4 h-4 mr-2" aria-hidden="true" /> Cancelar estudio
            </Button>
          )}
          {canDelete(study.status) && (
            <Button variant="outline" className="min-h-[48px] text-[var(--color-danger)]"
              onClick={() => setDialog('delete')}>
              <Trash2 className="w-4 h-4 mr-2" aria-hidden="true" /> Eliminar estudio
            </Button>
          )}
          {step && (
            <Button className="min-h-[48px]" onClick={() => setDialog('status')}>{step.action}</Button>
          )}
        </div>
      </div>

      {study.status === 'CANCELADO' && (
        <AlertBanner variant="warning" title="Estudio cancelado"
          message={`Motivo: ${study.cancellationReason ?? 'sin motivo registrado'}. Sus datos se conservan, pero no aparece en Registro en campo ni en Indicadores.`} />
      )}
      {rejection && (
        <AlertBanner variant="danger" title="No se pudo completar la acción" message={rejection}
          onDismiss={() => setRejection(null)} />
      )}

      {study.status === 'PLANIFICADO' && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CalendarDays className="w-5 h-5" aria-hidden="true" /> Fechas de los días
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {datesSaved && !datesChanged && (
              <AlertBanner variant="success" message="Fechas guardadas." onDismiss={() => setDatesSaved(false)} />
            )}
            <StudyDatesEditor dates={editedDates} onChange={(next) => { setDates(next); setDatesSaved(false) }}
              disabled={isSaving} />
            <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
              <Button variant="outline" className="min-h-[48px]" disabled={!datesChanged || isSaving}
                onClick={() => setDates(null)}>
                Descartar
              </Button>
              <Button className="min-h-[48px]" disabled={!datesChanged || isSaving || !editedDates[0]}
                isLoading={isSaving} onClick={handleSaveDates}>
                Guardar fechas
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Avance de los 8 días</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {study.days.map((day) => {
              const morning = slots.find((slot) => slot.dayNumber === day.dayNumber && slot.type === 'MANANA')
              const afternoon = slots.find((slot) => slot.dayNumber === day.dayNumber && slot.type === 'TARDE')
              return (
                <div key={day.dayNumber} className="rounded-lg border border-slate-200 p-3 flex flex-col gap-2"
                  data-testid={`study-day-${day.dayNumber}`}>
                  <p className="font-semibold text-sm">Día {day.dayNumber}</p>
                  <p className="text-xs text-[var(--color-text-muted)]">{formatDate(day.date)}</p>
                  {day.discarded && (
                    <p className="text-xs font-medium text-[var(--color-edu-blue)]">Día de descarte: no entra en los cálculos</p>
                  )}
                  <span className={cn('text-xs px-2 py-1 rounded border', slotStyle(morning))}>
                    M · {slotLabel(morning)}
                  </span>
                  <span className={cn('text-xs px-2 py-1 rounded border', slotStyle(afternoon))}>
                    T · {slotLabel(afternoon)}
                  </span>
                </div>
              )
            })}
          </div>
        </CardContent>
      </Card>

      {step && (
        <Modal open={dialog === 'status'} onClose={closeDialog} title={step.action} description={step.confirmation}>
          <div className="flex flex-col gap-4 mt-2">
            {step.needsReason && (
              <Input id="status-reason" label="Motivo" value={reason} onChange={(event) => setReason(event.target.value)}
                disabled={isSaving} required />
            )}
            <div className="flex justify-end gap-3">
              <Button variant="outline" className="min-h-[48px]" onClick={closeDialog} disabled={isSaving}>
                Volver
              </Button>
              <Button className="min-h-[48px]" onClick={handleStatus} isLoading={isSaving}
                disabled={isSaving || (step.needsReason && !reason.trim())}>
                Confirmar
              </Button>
            </div>
          </div>
        </Modal>
      )}

      <Modal open={dialog === 'cancel'} onClose={closeDialog} title="Cancelar estudio"
        description="La cancelación es definitiva: el estudio deja de aparecer en Registro en campo y en Indicadores, y sus fechas quedan libres para otro estudio. Los datos se conservan.">
        <div className="flex flex-col gap-4 mt-2">
          <Input id="cancel-reason" label="Motivo de la cancelación" value={reason}
            onChange={(event) => setReason(event.target.value)} disabled={isSaving} required />
          <div className="flex justify-end gap-3">
            <Button variant="outline" className="min-h-[48px]" onClick={closeDialog} disabled={isSaving}>Volver</Button>
            <Button variant="danger" className="min-h-[48px]" onClick={handleCancel} isLoading={isSaving}
              disabled={isSaving || !reason.trim()}>
              Cancelar estudio
            </Button>
          </div>
        </div>
      </Modal>

      <Modal open={dialog === 'delete'} onClose={closeDialog} title="Eliminar estudio"
        description="Se eliminan el estudio, sus fechas y sus fuentes participantes. No se puede deshacer.">
        <div className="flex justify-end gap-3 mt-2">
          <Button variant="outline" className="min-h-[48px]" onClick={closeDialog} disabled={isSaving}>Volver</Button>
          <Button variant="danger" className="min-h-[48px]" onClick={handleDelete} isLoading={isSaving}
            disabled={isSaving}>
            Eliminar estudio
          </Button>
        </div>
      </Modal>
    </div>
  )
}
