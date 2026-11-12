/**
 * Field home: the study in execution, the suggested day and shift, and the
 * button to open or continue it (Spec 004, T317; R-340, R-341, CA-300).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { PlayCircle, ArrowRight, Clock } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent, Modal, Select } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { hasPermission } from '@/features/auth/utils/permissions'
import { adminService } from '@/features/admin/services/adminService'
import { studyService } from '@/features/study/services/studyService'
import { formatDate } from '@/features/study/utils/samplingDays'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, Crew, ShiftSlot, ShiftType } from '@/types'
import { fieldService } from '../services/fieldService'
import { suggestSlot } from '../utils/fieldMath'

const SHIFT_LABELS: Record<ShiftType, string> = { MANANA: 'Mañana', TARDE: 'Tarde' }

export interface FieldDashboardProps {
  /** Current moment; tests fix it to choose the suggested shift. */
  now?: Date
}

export function FieldDashboard({ now }: FieldDashboardProps = {}) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const accessDenied = location.state?.accessDenied as boolean | undefined
  const canOpen = hasPermission(user?.role, 'MANAGE_SHIFTS')

  const [study, setStudy] = useState<CharacterizationStudy | null | undefined>(undefined)
  const [slots, setSlots] = useState<ShiftSlot[]>([])
  const [dayNumber, setDayNumber] = useState(1)
  const [type, setType] = useState<ShiftType>('MANANA')
  const [crews, setCrews] = useState<Crew[]>([])
  const [crewId, setCrewId] = useState<string>('')
  const [isOpening, setIsOpening] = useState(false)
  const [isConfirming, setIsConfirming] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const running = (await studyService.list()).find((item) => item.status === 'EN_EJECUCION') ?? null
      setStudy(running)
      if (running) {
        const loaded = await studyService.shifts(running.id)
        setSlots(loaded)
        const suggestion = suggestSlot(running, loaded, now ?? new Date())
        setDayNumber(suggestion.dayNumber)
        setType(suggestion.type)
      }
    } catch {
      setError('No se pudo cargar el estudio en ejecución.')
      setStudy(null)
    }
  }, [now])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (canOpen) {
      adminService.getCrews().then(setCrews).catch(() => setCrews([]))
    }
  }, [canOpen])

  const slot = useMemo(
    () => slots.find((item) => item.dayNumber === dayNumber && item.type === type),
    [slots, dayNumber, type]
  )
  const day = study?.days.find((item) => item.dayNumber === dayNumber)

  const handleOpen = async () => {
    if (!study) return
    setError(null)
    try {
      setIsOpening(true)
      const shift = await fieldService.openShift(study.id, dayNumber, type, crewId ? Number(crewId) : null)
      navigate(`/campo/turno/${shift.id}`)
    } catch (err) {
      setIsConfirming(false)
      setError(err instanceof ApiError ? err.message : 'No se pudo abrir el turno.')
    } finally {
      setIsOpening(false)
    }
  }

  return (
    <div className="max-w-3xl w-full mx-auto flex flex-col gap-6">
      {accessDenied && (
        <AlertBanner
          variant="warning"
          title="Acceso restringido"
          message="Acceso denegado: únicamente los docentes tienen permisos para acceder al módulo de administración y configuración."
        />
      )}

      <div>
        <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Registro en campo</h1>
        {study && <p className="text-sm text-[var(--color-text-muted)]">{studyTitle(study)}</p>}
      </div>

      {error && <AlertBanner variant="danger" title="No se pudo continuar" message={error} />}

      {study === undefined && <p className="text-sm text-[var(--color-text-muted)]">Cargando estudio...</p>}

      {study === null && !error && (
        <AlertBanner
          variant="info"
          title="No hay un estudio en ejecución"
          message={canOpen
            ? 'Inicie la ejecución de un estudio desde Configuración del estudio para registrar datos en campo.'
            : 'Cuando el docente inicie la ejecución del estudio, aquí podrá registrar los datos del turno.'}
        />
      )}

      {study && (
        <Card>
          <CardContent className="flex flex-col gap-5 py-5">
            <div className="grid grid-cols-2 gap-3">
              <Select
                id="field-day"
                label="Día de muestreo"
                value={String(dayNumber)}
                onChange={(event) => setDayNumber(Number(event.target.value))}
                options={study.days.map((item) => ({
                  value: String(item.dayNumber),
                  label: `Día ${item.dayNumber}${item.discarded ? ' (descarte)' : ''}`,
                }))}
              />
              <Select
                id="field-shift"
                label="Turno"
                value={type}
                onChange={(event) => setType(event.target.value as ShiftType)}
                options={[{ value: 'MANANA', label: 'Mañana' }, { value: 'TARDE', label: 'Tarde' }]}
              />
            </div>
            <p className="text-sm text-[var(--color-text-muted)] flex items-center gap-2">
              <Clock className="w-4 h-4" /> {formatDate(day?.date)} · Turno de la {SHIFT_LABELS[type].toLowerCase()}
            </p>

            {slot?.shiftId ? (
              <Button className="min-h-[56px] text-lg" onClick={() => navigate(`/campo/turno/${slot.shiftId}`)}>
                {slot.status === 'CERRADO' ? 'Ver turno cerrado' : 'Continuar turno'}
                <ArrowRight className="w-5 h-5 ml-2" />
              </Button>
            ) : canOpen ? (
              <Button className="min-h-[56px] text-lg" onClick={() => setIsConfirming(true)}>
                <PlayCircle className="w-5 h-5 mr-2" /> Abrir turno
              </Button>
            ) : (
              <AlertBanner
                variant="info"
                title="Turno no iniciado"
                message="Espere a que el docente abra el turno para registrar datos."
              />
            )}
          </CardContent>
        </Card>
      )}

      <Modal open={isConfirming} onClose={() => setIsConfirming(false)} title="Abrir turno"
        description={`Día ${dayNumber} · ${SHIFT_LABELS[type]} · ${formatDate(day?.date)}`}>
        <div className="flex flex-col gap-4 mt-2">
          <Select
            id="field-crew"
            label="Cuadrilla"
            value={crewId}
            onChange={(event) => setCrewId(event.target.value)}
            options={[{ value: '', label: 'Sin cuadrilla asignada' },
              ...crews.map((crew) => ({ value: String(crew.id), label: crew.name }))]}
            disabled={isOpening}
          />
          <div className="flex justify-end gap-3">
            <Button variant="outline" className="min-h-[48px]" onClick={() => setIsConfirming(false)} disabled={isOpening}>
              Cancelar
            </Button>
            <Button className="min-h-[48px]" onClick={handleOpen} isLoading={isOpening} disabled={isOpening}>
              Confirmar apertura
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
