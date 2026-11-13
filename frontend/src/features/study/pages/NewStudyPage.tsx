/**
 * New characterization study: dates of the 8 days and participating sources
 * (Spec 004, T314 and T315; R-330, R-331, R-333, CA-301).
 * Crews are assigned when each shift is opened (R-341), because the API keeps
 * the crew in the shift.
 *
 * @author craulgomez
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CalendarDays, Info } from 'lucide-react'
import { AlertBanner, BackButton, Button, Card, CardContent, CardHeader, CardTitle, Input } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { useAuth } from '@/features/auth'
import { adminService } from '@/features/admin/services/adminService'
import { ApiError } from '@/services/apiClient'
import { GenerationSource } from '@/types'
import { studyService } from '../services/studyService'
import { calculateSamplingDays, formatDate, isWeekend } from '../utils/samplingDays'

const DAY_COUNT = 8

export function NewStudyPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const institutionId = user?.institutionId ?? 1
  const [dates, setDates] = useState<string[]>(Array(DAY_COUNT).fill(''))
  const [sources, setSources] = useState<GenerationSource[]>([])
  const [selected, setSelected] = useState<Set<number>>(new Set())
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const { confirmLeave, discardDialog } = useUnsavedChanges(!isSaving && dates.some((date) => date !== ''))

  useEffect(() => {
    adminService.getSources(institutionId)
      .then((all) => {
        const active = all.filter((source) => source.active)
        setSources(active)
        setSelected(new Set(active.map((source) => source.id)))
      })
      .catch(() => setError('No se pudieron cargar las fuentes de generación.'))
  }, [institutionId])

  const weekendDays = useMemo(
    () => dates.map((date, index) => (date && isWeekend(date) ? index + 1 : null)).filter(Boolean),
    [dates]
  )

  const handleFirstDay = (firstDay: string) => {
    setDates(firstDay ? [firstDay, ...calculateSamplingDays(firstDay)] : Array(DAY_COUNT).fill(''))
  }

  const handleDay = (index: number, value: string) => {
    setDates((previous) => previous.map((date, position) => (position === index ? value : date)))
  }

  const toggleSource = (id: number) => {
    setSelected((previous) => {
      const next = new Set(previous)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }
      return next
    })
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    if (!dates[0]) {
      setError('Elija la fecha del día 1.')
      return
    }
    if (selected.size === 0) {
      setError('Elija al menos una fuente participante.')
      return
    }
    try {
      setIsSaving(true)
      const study = await studyService.create(
        institutionId,
        dates.map((date, index) => ({ dayNumber: index + 1, date: date || null }))
      )
      const sameSources = study.participatingSourceIds.length === selected.size
        && study.participatingSourceIds.every((id) => selected.has(id))
      if (!sameSources) {
        await studyService.setSources(study.id, [...selected])
      }
      navigate(`/estudios/${study.id}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo crear el estudio.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="max-w-4xl w-full mx-auto flex flex-col gap-6">
      <BackButton to="/estudios" label="Atrás a los estudios" onLeave={confirmLeave} />
      <div>
        <h1 className="text-2xl font-bold text-[var(--color-brand-dark)]">Nuevo estudio de caracterización</h1>
        <p className="text-sm text-[var(--color-text-muted)]">
          8 días de muestreo con turnos de mañana y tarde. Las cuadrillas se asignan al abrir cada turno.
        </p>
      </div>

      {error && <AlertBanner variant="danger" title="No se pudo guardar" message={error} />}

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CalendarDays className="w-5 h-5" /> Días de muestreo
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <AlertBanner
              variant="info"
              title="Día 1: Día de descarte: no entra en los cálculos"
              message="Los residuos del día 1 se acumularon antes del estudio; se pesan pero no cuentan para la línea base."
            />
            <p className="text-sm text-[var(--color-text-muted)]">
              Al elegir el día 1 se proponen los 7 días hábiles siguientes (lunes a viernes). Cambie los que caigan en
              festivos o jornadas especiales.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {dates.map((date, index) => (
                <Input
                  key={index}
                  id={`study-day-${index + 1}`}
                  type="date"
                  label={index === 0 ? 'Día 1 (descarte)' : `Día ${index + 1}`}
                  value={date}
                  onChange={(event) => (index === 0 ? handleFirstDay(event.target.value) : handleDay(index, event.target.value))}
                  helperText={date ? formatDate(date) : undefined}
                  disabled={isSaving}
                  required={index === 0}
                />
              ))}
            </div>
            {weekendDays.length > 0 && (
              <AlertBanner
                variant="warning"
                message={`Los días ${weekendDays.join(', ')} caen en fin de semana. Revise que haya clases esos días.`}
              />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Fuentes participantes</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <p className="text-sm text-[var(--color-text-muted)] flex items-start gap-2">
              <Info className="w-4 h-4 mt-0.5 shrink-0" /> Por defecto participan todas las fuentes activas.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {sources.map((source) => (
                <label
                  key={source.id}
                  className="flex items-center gap-3 min-h-[48px] px-3 rounded-lg border border-slate-200 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={selected.has(source.id)}
                    onChange={() => toggleSource(source.id)}
                    disabled={isSaving}
                    className="w-5 h-5"
                    aria-label={`${source.code} ${source.name}`}
                  />
                  <span className="font-mono font-bold">{source.code}</span>
                  <span className="text-sm">{source.name}</span>
                </label>
              ))}
            </div>
          </CardContent>
        </Card>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" className="min-h-[48px]"
            onClick={() => confirmLeave(() => navigate('/estudios'))} disabled={isSaving}>
            Descartar
          </Button>
          <Button type="submit" className="min-h-[48px]" isLoading={isSaving} disabled={isSaving}>
            Crear estudio
          </Button>
        </div>
      </form>
      {discardDialog}
    </div>
  )
}
