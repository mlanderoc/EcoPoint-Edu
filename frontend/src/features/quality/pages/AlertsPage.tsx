/**
 * Data-quality alerts: filters, link to the record that caused each one and
 * attention with a mandatory note (Spec 005, T407; R-430, R-431, CA-411).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertBanner, Button, Card, CardContent, Input, Modal, Select } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { Alert, AlertType } from '@/types'
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
  const [state, setState] = useState('false')
  const [type, setType] = useState('')
  const [attending, setAttending] = useState<Alert | null>(null)
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setAlerts(await alertService.list({
        attended: state === '' ? undefined : state === 'true',
        type: (type || undefined) as AlertType | undefined,
      }))
    } catch {
      setError('No se pudieron cargar las alertas.')
    }
  }, [state, type])

  useEffect(() => { load() }, [load])

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

  return (
    <div className="max-w-3xl w-full mx-auto flex flex-col gap-5">
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Alertas de calidad</h1>
      {error && <AlertBanner variant="danger" message={error} />}
      <div className="grid grid-cols-2 gap-3">
        <Select id="alerts-state" label="Estado" value={state} onChange={(event) => setState(event.target.value)}
          options={[{ value: 'false', label: 'Pendientes' }, { value: 'true', label: 'Atendidas' }, { value: '', label: 'Todas' }]} />
        <Select id="alerts-type" label="Tipo" value={type} onChange={(event) => setType(event.target.value)}
          options={[{ value: '', label: 'Todos los tipos' },
            ...Object.entries(TYPE_LABELS).map(([value, label]) => ({ value, label }))]} />
      </div>
      {alerts.length === 0 && <p className="text-sm text-slate-500">No hay alertas con estos filtros.</p>}
      {alerts.map((alert) => (
        <Card key={alert.id}>
          <CardContent className="flex flex-col gap-2 py-4" data-testid={`alert-${alert.id}`}>
            <div className="flex flex-wrap items-center gap-2">
              <Badge tone={alert.type === 'CUARTEO_NO_CONFORME' ? 'danger' : 'warning'}>{TYPE_LABELS[alert.type]}</Badge>
              <Badge tone={alert.attended ? 'success' : 'neutral'}>{alert.attended ? 'Atendida' : 'Pendiente'}</Badge>
            </div>
            <p className="text-sm">{alert.description}</p>
            {alert.attended && (
              <p className="text-xs text-slate-500">
                Atendida por el usuario {alert.attendedBy} el {alert.attendedAt ? new Date(alert.attendedAt).toLocaleString('es-CO') : ''}: {alert.note}
              </p>
            )}
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" className="min-h-[48px]" onClick={() => openRecord(alert)}>Ver registro</Button>
              {!alert.attended && (
                <Button className="min-h-[48px]" onClick={() => { setAttending(alert); setNote(''); setNoteError(null) }}>
                  Marcar atendida
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      ))}
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
