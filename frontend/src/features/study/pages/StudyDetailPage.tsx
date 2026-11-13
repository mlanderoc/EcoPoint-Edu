/**
 * Lifecycle of a study: progress of its 8 days and the step to the next status
 * (Spec 004, T316; R-332, R-333, CA-309).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AlertBanner, BackButton, Button, Card, CardContent, CardHeader, CardTitle, Input, Modal } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, ShiftSlot } from '@/types'
import { cn } from '@/utils/cn'
import { studyService } from '../services/studyService'
import { formatDate } from '../utils/samplingDays'
import { NEXT_STEP, STATUS_LABELS, studyTitle } from '../utils/studyStatus'

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

export function StudyDetailPage() {
  const { id } = useParams()
  const studyId = Number(id)
  const [study, setStudy] = useState<CharacterizationStudy | null>(null)
  const [slots, setSlots] = useState<ShiftSlot[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [rejection, setRejection] = useState<string | null>(null)
  const [isConfirming, setIsConfirming] = useState(false)
  const [reason, setReason] = useState('')
  const [isSaving, setIsSaving] = useState(false)

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

  const handleConfirm = async () => {
    setRejection(null)
    try {
      setIsSaving(true)
      const updated = await studyService.changeStatus(study.id, step.target, step.needsReason ? reason : undefined)
      setStudy(updated)
      setIsConfirming(false)
      setReason('')
    } catch (err) {
      setIsConfirming(false)
      setRejection(err instanceof ApiError ? err.message : 'No se pudo cambiar el estado del estudio.')
    } finally {
      setIsSaving(false)
    }
  }

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
        <Button className="min-h-[48px]" onClick={() => setIsConfirming(true)}>
          {step.action}
        </Button>
      </div>

      {rejection && (
        <AlertBanner
          variant="danger"
          title="No se pudo cambiar el estado"
          message={rejection}
          onDismiss={() => setRejection(null)}
        />
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

      <Modal open={isConfirming} onClose={() => setIsConfirming(false)} title={step.action}
        description={step.confirmation}>
        <div className="flex flex-col gap-4 mt-2">
          {step.needsReason && (
            <Input id="status-reason" label="Motivo" value={reason} onChange={(event) => setReason(event.target.value)}
              disabled={isSaving} required />
          )}
          <div className="flex justify-end gap-3">
            <Button variant="outline" className="min-h-[48px]" onClick={() => setIsConfirming(false)} disabled={isSaving}>
              Cancelar
            </Button>
            <Button className="min-h-[48px]" onClick={handleConfirm} isLoading={isSaving}
              disabled={isSaving || (step.needsReason && !reason.trim())}>
              Confirmar
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
