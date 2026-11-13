/**
 * A shift: its population, the checklist of participating sources and its
 * closing (Spec 004, T322 and T323; R-345, R-346, CA-306).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, CircleDashed, Lock, MinusCircle, Plus, Users } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent, CardHeader, CardTitle, Modal } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { hasPermission } from '@/features/auth/utils/permissions'
import { ApiError } from '@/services/apiClient'
import { QualityStatus, SamplingShift, SourceProgress } from '@/types'
import { qualityService } from '@/features/quality/services/qualityService'
import { cn } from '@/utils/cn'
import { fieldService } from '../services/fieldService'
import { formatNumber, plural } from '../utils/fieldMath'

const STATUS_TEXT: Record<SourceProgress['status'], string> = {
  CON_GENERACION: 'Con bolsas',
  SIN_GENERACION: 'Sin generación',
  PENDIENTE: 'Pendiente',
}

function StatusBadge({ status }: { status: SourceProgress['status'] }) {
  const styles = {
    CON_GENERACION: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    SIN_GENERACION: 'bg-slate-100 text-slate-700 border-slate-300',
    PENDIENTE: 'bg-amber-50 text-amber-900 border-amber-300',
  }
  const icons = {
    CON_GENERACION: <CheckCircle2 className="w-4 h-4" />,
    SIN_GENERACION: <MinusCircle className="w-4 h-4" />,
    PENDIENTE: <CircleDashed className="w-4 h-4" />,
  }
  return (
    <span className={cn('inline-flex items-center gap-1 text-xs font-semibold px-2 py-1 rounded-full border', styles[status])}>
      {icons[status]} {STATUS_TEXT[status]}
    </span>
  )
}

export function ShiftPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const canClose = hasPermission(user?.role, 'MANAGE_SHIFTS')
  const [shift, setShift] = useState<SamplingShift | null>(null)
  const [quarteringStatus, setQuarteringStatus] = useState<QualityStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busySource, setBusySource] = useState<number | null>(null)
  const [isConfirming, setIsConfirming] = useState(false)
  const [isClosing, setIsClosing] = useState(false)

  const load = useCallback(async () => {
    try {
      const loaded = await fieldService.getShift(shiftId)
      setShift(loaded)
      if (loaded.hasQuartering) {
        setQuarteringStatus((await qualityService.getQuartering(shiftId))?.qualityStatus ?? null)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo cargar el turno.')
    }
  }, [shiftId])

  useEffect(() => {
    load()
  }, [load])

  if (!shift) {
    return error
      ? <AlertBanner variant="danger" title="Turno no disponible" message={error} />
      : <p className="text-sm text-[var(--color-text-muted)]">Cargando turno...</p>
  }

  const isOpen = shift.status === 'ABIERTO'
  const pending = shift.sources.filter((source) => source.status === 'PENDIENTE')
  const withoutGeneration = shift.sources.filter((source) => source.status === 'SIN_GENERACION')

  const markNoGeneration = async (sourceId: number, noGeneration: boolean) => {
    setError(null)
    try {
      setBusySource(sourceId)
      setShift(await fieldService.setNoGeneration(shiftId, sourceId, noGeneration))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo actualizar la fuente.')
    } finally {
      setBusySource(null)
    }
  }

  const handleClose = async () => {
    setError(null)
    try {
      setIsClosing(true)
      setShift(await fieldService.closeShift(shiftId))
      setIsConfirming(false)
    } catch (err) {
      setIsConfirming(false)
      setError(err instanceof ApiError ? err.message : 'No se pudo cerrar el turno.')
    } finally {
      setIsClosing(false)
    }
  }

  return (
    <div className="max-w-3xl w-full mx-auto flex flex-col gap-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Turno {shift.code}</h1>
          <p className="text-sm text-[var(--color-text-muted)]">
            {isOpen ? 'Abierto' : 'Cerrado'} · {plural(shift.bagCount, 'bolsa')} · {formatNumber(shift.totalNetWeight)} kg netos
          </p>
        </div>
        {isOpen && shift.population && (
          <Button className="min-h-[56px]" onClick={() => navigate(`/campo/turno/${shiftId}/bolsas/nueva`)}>
            <Plus className="w-5 h-5 mr-2" /> Registrar bolsa
          </Button>
        )}
      </div>

      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}
      {quarteringStatus === 'NO_CONFORME' && (
        <AlertBanner variant="danger" title="Cuarteo no conforme: repesar"
          message="La diferencia supera la tolerancia del 2 %. Revise el cuarteo." />
      )}

      <Card>
        <CardContent className="flex flex-wrap items-center justify-between gap-3 py-4">
          <div className="flex items-center gap-3">
            <Users className="w-5 h-5 text-[var(--color-primary)]" />
            {shift.population ? (
              <p className="text-sm">
                Población: <strong>{shift.population.total}</strong> personas ({shift.population.students} estudiantes,{' '}
                {shift.population.teachers} docentes, {shift.population.staff} personal)
              </p>
            ) : (
              <p className="text-sm font-medium text-amber-900">Registre primero la población del turno.</p>
            )}
          </div>
          {isOpen && !shift.population && (
            <Button className="min-h-[48px]" onClick={() => navigate(`/campo/turno/${shiftId}/poblacion`)}>
              Registrar población
            </Button>
          )}
        </CardContent>
      </Card>

      {shift.population && shift.bagCount > 0 && (
        <div className="grid grid-cols-2 gap-3">
          <Button variant="outline" className="min-h-[56px]" onClick={() => navigate(`/campo/turno/${shiftId}/cuarteo`)}>
            {shift.hasQuartering ? 'Ver cuarteo' : 'Cuarteo'}
          </Button>
          <Button variant="outline" className="min-h-[56px]" onClick={() => navigate(`/campo/turno/${shiftId}/densidad`)}>
            {shift.hasDensity ? 'Ver densidad' : 'Densidad'}
          </Button>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Fuentes del turno</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col divide-y divide-slate-100">
          {shift.sources.map((source) => (
            <div key={source.sourceId} className="flex flex-wrap items-center justify-between gap-3 py-3"
              data-testid={`source-${source.code}`}>
              <div className="min-w-0">
                <p className="font-mono font-bold text-lg">{source.code}</p>
                <p className="text-sm text-[var(--color-text-muted)]">{source.name}</p>
                {source.bagCount > 0 && (
                  <p className="text-xs">{plural(source.bagCount, 'bolsa')} · {formatNumber(source.totalNetWeight)} kg</p>
                )}
              </div>
              <div className="flex items-center gap-2">
                <StatusBadge status={source.status} />
                {isOpen && source.status === 'PENDIENTE' && (
                  <Button variant="outline" className="min-h-[48px]" disabled={busySource !== null}
                    isLoading={busySource === source.sourceId} onClick={() => markNoGeneration(source.sourceId, true)}>
                    Marcar sin generación
                  </Button>
                )}
                {isOpen && source.status === 'SIN_GENERACION' && (
                  <Button variant="ghost" className="min-h-[48px]" disabled={busySource !== null}
                    onClick={() => markNoGeneration(source.sourceId, false)}>
                    Quitar marca
                  </Button>
                )}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      {canClose && isOpen && (
        <div className="flex flex-col gap-2">
          {pending.length > 0 && (
            <p className="text-sm text-amber-900 flex items-center gap-2">
              <Lock className="w-4 h-4" /> Faltan {plural(pending.length, 'fuente')}: {pending.map((source) => source.code).join(', ')}.
              Registre sus bolsas o márquelas sin generación.
            </p>
          )}
          <Button variant="danger" className="min-h-[56px]" disabled={pending.length > 0 || !shift.population}
            onClick={() => setIsConfirming(true)}>
            Cerrar turno
          </Button>
        </div>
      )}

      <Modal open={isConfirming} onClose={() => setIsConfirming(false)} title={`Cerrar turno ${shift.code}`}
        description="Revise el resumen. Un turno cerrado ya no admite registros.">
        <div className="flex flex-col gap-4 mt-2" data-testid="close-summary">
          <ul className="text-sm flex flex-col gap-1">
            <li>Población: <strong>{shift.population?.total ?? 0}</strong> personas</li>
            <li>Bolsas: <strong>{shift.bagCount}</strong></li>
            <li>Kilogramos netos: <strong>{formatNumber(shift.totalNetWeight)}</strong></li>
            <li>
              Sin generación: <strong>{withoutGeneration.length > 0
                ? withoutGeneration.map((source) => source.code).join(', ') : 'ninguna'}</strong>
            </li>
          </ul>
          <div className="flex justify-end gap-3">
            <Button variant="outline" className="min-h-[48px]" onClick={() => setIsConfirming(false)} disabled={isClosing}>
              Cancelar
            </Button>
            <Button variant="danger" className="min-h-[48px]" onClick={handleClose} isLoading={isClosing}
              disabled={isClosing}>
              Confirmar cierre
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
