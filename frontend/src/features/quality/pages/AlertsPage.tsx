/**
 * Data-quality alerts: filters, link to the record that caused each one and
 * attention with a mandatory note (Spec 005, T407; R-430, R-431, CA-411).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertBanner, Button, Input, Modal, Select } from '@/components/ui'
import { useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { studyService } from '@/features/study/services/studyService'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { ApiError } from '@/services/apiClient'
import { Alert, AlertType, CharacterizationStudy } from '@/types'
import { Badge } from '../components/Badge'
import { alertService, recordTypeOf } from '../services/verificationService'

const TYPE_LABELS: Record<AlertType, string> = {
  CUARTEO_NO_CONFORME: 'Cuarteo no conforme',
  VALOR_ATIPICO: 'Valor atípico',
  VERIFICACION_PENDIENTE: 'Verificación pendiente',
}

export function AlertsPage() {
  const navigate = useNavigate()
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [attendedTab, setAttendedTab] = useState(false)
  const [type, setType] = useState('')
  const [attending, setAttending] = useState<Alert | null>(null)
  const { institutions } = useInstitutions(false)
  const [studies, setStudies] = useState<CharacterizationStudy[]>([])
  const [studyId, setStudyId] = useState('')
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setAlerts(await alertService.list({
        studyId: studyId ? Number(studyId) : undefined,
        type: (type || undefined) as AlertType | undefined,
      }))
    } catch {
      setError('No se pudieron cargar las alertas.')
    }
  }, [studyId, type])

  useEffect(() => { load() }, [load])

  useEffect(() => {
    studyService.visible(institutions.map((institution) => institution.id))
      .then((all) => setStudies(all.filter((study) => study.status !== 'CANCELADO')))
      .catch(() => setStudies([]))
  }, [institutions])

  const openRecord = (alert: Alert) => {
    const recordType = recordTypeOf(alert.targetType)
    navigate(recordType ? `/calidad/registro/${recordType}/${alert.targetId}` : `/campo/turno/${alert.targetId}`)
  }

  const handleAttend = async () => {
    if (!attending) return
    setNoteError(null)
    if (!note.trim()) {
      setNoteError('Escriba una observación sobre cómo se atendió la alerta.')
      return
    }
    try {
      await alertService.attend(attending.id, note.trim())
      setAttending(null)
      setNote('')
      await load()
    } catch (err) {
      setNoteError(err instanceof ApiError ? (err.getFieldError('reason') ?? err.message) : 'No se pudo atender la alerta.')
    }
  }

  const pending = alerts.filter((alert) => !alert.attended)
  const attended = alerts.filter((alert) => alert.attended)
  const shown = attendedTab ? attended : pending

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Alertas de calidad</h1>
          <p className="m-0 mt-1">Datos que necesitan una segunda mirada.</p>
        </div>
      </div>
      {error && <AlertBanner variant="danger" message={error} />}
      <div className="toolbar">
        <div className="segmented" role="group" aria-label="Filtrar alertas">
          <button type="button" aria-pressed={!attendedTab} onClick={() => setAttendedTab(false)}>Pendientes ({pending.length})</button>
          <button type="button" aria-pressed={attendedTab} onClick={() => setAttendedTab(true)}>Atendidas ({attended.length})</button>
        </div>
        <div className="w-full sm:w-72">
          <Select id="alerts-study" aria-label="Estudio de la alerta" value={studyId}
            onChange={(event) => setStudyId(event.target.value)}
            options={[{ value: '', label: 'Todos los estudios' },
              ...studies.map((study) => {
                const name = institutions.find((item) => item.id === study.institutionId)?.name
                return { value: String(study.id), label: name ? `${studyTitle(study)} · ${name}` : studyTitle(study) }
              })]} />
        </div>
        <div className="w-full sm:w-64">
          <Select id="alerts-type" aria-label="Tipo de alerta" value={type} onChange={(event) => setType(event.target.value)}
            options={[{ value: '', label: 'Todos los tipos' },
              ...Object.entries(TYPE_LABELS).map(([value, label]) => ({ value, label }))]} />
        </div>
      </div>
      <section className="panel">
        {shown.length === 0 && (
          <div className="empty">
            <h3>Todo en orden</h3>
            <p className="m-0 text-[var(--muted)]">{attendedTab ? 'No hay alertas atendidas.' : 'No hay alertas pendientes.'}</p>
          </div>
        )}
        {shown.map((alert) => (
          <article key={alert.id} className="alert-card" data-testid={`alert-${alert.id}`}>
            <span className="mark">{alert.attended ? '✓' : '!'}</span>
            <div className="flex flex-col gap-2 min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <Badge tone={alert.type === 'CUARTEO_NO_CONFORME' ? 'danger' : 'warning'}>{TYPE_LABELS[alert.type]}</Badge>
                <Badge tone={alert.attended ? 'success' : 'neutral'}>{alert.attended ? 'Atendida' : 'Pendiente'}</Badge>
              </div>
              <p className="font-bold">{alert.description}</p>
              {alert.attended && (
                <blockquote>
                  Atendida por {alert.attendedByName ?? `el usuario ${alert.attendedBy}`} el {alert.attendedAt ? new Date(alert.attendedAt).toLocaleString('es-CO') : ''}: {alert.note}
                </blockquote>
              )}
              <div className="flex flex-wrap gap-2">
                <Button variant="outline" className="min-h-[48px]" onClick={() => openRecord(alert)}>Ver registro</Button>
                {!alert.attended && (
                  <Button className="min-h-[48px]" onClick={() => { setAttending(alert); setNote(''); setNoteError(null) }}>
                    Marcar atendida
                  </Button>
                )}
              </div>
            </div>
          </article>
        ))}
      </section>
      <Modal open={attending !== null} onClose={() => setAttending(null)} title="Atender alerta"
        description="Escriba qué se hizo. La alerta atendida queda en el historial con su nombre y la fecha.">
        <div className="flex flex-col gap-4 mt-2">
          <Input id="alert-note" label="Observación" value={note} error={noteError ?? undefined}
            onChange={(event) => setNote(event.target.value)} />
          <div className="flex justify-end gap-3">
            <Button variant="outline" className="min-h-[48px]" onClick={() => setAttending(null)}>Cancelar</Button>
            <Button className="min-h-[48px]" onClick={handleAttend}>Guardar</Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
