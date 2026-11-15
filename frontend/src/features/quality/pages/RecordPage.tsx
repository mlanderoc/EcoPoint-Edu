/**
 * One verifiable record: its values, the correction against the paper form
 * with a mandatory reason, and its history (Spec 005, T406; R-422, R-425,
 * CA-404, CA-409, CA-412).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AlertBanner, BackButton, Button, Card, CardContent, CardHeader, CardTitle, Input } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { parseDecimal } from '@/features/field/utils/fieldMath'
import { ApiError } from '@/services/apiClient'
import { CorrectionEntry, RecordType, VerifiableRecord } from '@/types'
import { RecordCard, formatValue } from '../components/RecordCard'
import { verificationService } from '../services/verificationService'

export function RecordPage() {
  const { tipo, id } = useParams()
  const type = tipo as RecordType
  const recordId = Number(id)
  const { user } = useAuth()
  const [record, setRecord] = useState<VerifiableRecord | null>(null)
  const [history, setHistory] = useState<CorrectionEntry[]>([])
  const [values, setValues] = useState<Record<string, string>>({})
  const [reason, setReason] = useState('')
  const [reasonError, setReasonError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const [loaded, loadedHistory] = await Promise.all([
        verificationService.record(type, recordId), verificationService.history(type, recordId)])
      setRecord(loaded)
      setHistory(loadedHistory)
      setValues(Object.fromEntries(loaded.fields.filter((field) => field.editable)
        .map((field) => [field.field, String(field.value ?? '').replace('.', ',')])))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar el registro.')
    }
  }, [type, recordId])

  useEffect(() => { load() }, [load])

  if (!record) {
    return error
      ? <AlertBanner variant="danger" title="Registro no disponible" message={error} />
      : <p className="text-sm text-[var(--color-text-muted)]">Cargando registro...</p>
  }

  const typedByMe = record.verification.enteredBy === user?.id

  const handleCorrect = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setReasonError(null)
    if (!reason.trim()) {
      setReasonError('El motivo de la corrección es obligatorio.')
      return
    }
    const changed: Record<string, number> = {}
    for (const field of record.fields.filter((item) => item.editable)) {
      const parsed = parseDecimal(values[field.field] ?? '')
      if (parsed !== null && parsed !== Number(field.value)) changed[field.field] = parsed
    }
    if (Object.keys(changed).length === 0) {
      setError('No hay cambios que corregir: los valores son iguales.')
      return
    }
    try {
      setIsSaving(true)
      await verificationService.correct(type, recordId, changed, reason.trim())
      setReason('')
      await load()
    } catch (err) {
      if (err instanceof ApiError && err.getFieldError('reason')) setReasonError(err.getFieldError('reason') ?? null)
      else setError(err instanceof ApiError ? err.message : 'No se pudo guardar la corrección.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="max-w-3xl w-full mx-auto flex flex-col gap-5">
      <BackButton to="/calidad/verificacion" label="Atrás a la verificación" />
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">{record.title}</h1>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}
      <RecordCard record={record}>
        <p className="text-xs text-slate-500">
          Digitado por el usuario {record.verification.enteredBy ?? '—'}
          {record.verification.enteredAt ? ` el ${new Date(record.verification.enteredAt).toLocaleString('es-CO')}` : ''}
        </p>
      </RecordCard>

      {typedByMe ? (
        <AlertBanner variant="info" message="Usted digitó este registro: la verificación o corrección la hace otra persona (doble registro)." />
      ) : (
        <Card>
          <CardHeader><CardTitle>Corregir contra la planilla</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={handleCorrect} className="flex flex-col gap-3">
              {record.fields.filter((field) => field.editable).map((field) => (
                <Input key={field.field} id={`correct-${field.field}`} label={field.label} inputMode="decimal"
                  value={values[field.field] ?? ''} disabled={isSaving} className="min-h-[48px]"
                  onChange={(event) => setValues((previous) => ({ ...previous, [field.field]: event.target.value }))} />
              ))}
              <Input id="correct-reason" label="Motivo de la corrección" value={reason} error={reasonError ?? undefined}
                onChange={(event) => setReason(event.target.value)} disabled={isSaving} className="min-h-[48px]" />
              <Button type="submit" className="min-h-[48px]" isLoading={isSaving} disabled={isSaving}>
                Guardar corrección
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader><CardTitle>Historial de correcciones</CardTitle></CardHeader>
        <CardContent>
          {history.length === 0 ? (
            <p className="text-sm text-slate-500">Sin correcciones.</p>
          ) : (
            <ul className="flex flex-col gap-2 text-sm" data-testid="correction-history">
              {history.map((entry, index) => {
                const label = record.fields.find((field) => field.field === entry.field)?.label ?? entry.field
                return (
                  <li key={index} className="border-b border-slate-100 pb-2">
                    <strong>{label}</strong>: {formatValue(Number(entry.oldValue))} → {formatValue(Number(entry.newValue))}
                    <br />Motivo: {entry.reason} · usuario {entry.userId} · {new Date(entry.at).toLocaleString('es-CO')}
                  </li>
                )
              })}
            </ul>
          )}
        </CardContent>
      </Card>

    </div>
  )
}
