/**
 * A shift: its population, the checklist of participating sources and its
 * closing (Spec 004, T322 and T323; R-345, R-346, CA-306).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { CheckCircle2, CircleDashed, Lock, MinusCircle, Plus, Users } from 'lucide-react'
import { AlertBanner, BackButton, Button, Modal } from '@/components/ui'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { hasPermission } from '@/features/auth/utils/permissions'
import { ApiError } from '@/services/apiClient'
import { QualityStatus, SamplingShift, SourceProgress } from '@/types'
import { qualityService } from '@/features/quality/services/qualityService'
import { cn } from '@/utils/cn'
import { BagIcon } from '@/features/home/components/BagIcon'
import { ShiftSteps } from '../components/ShiftSteps'
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
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <BackButton to="/campo" label="Atrás a Registro en Campo" />
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Turno {shift.code}</h1>
          <p className="m-0 mt-1">
            {isOpen ? 'Abierto' : 'Cerrado'} · {plural(shift.bagCount, 'bolsa')} · {formatNumber(shift.totalNetWeight)} kg netos
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {isOpen && shift.population && (
            <Button className="min-h-[48px]" onClick={() => navigate(`/campo/turno/${shiftId}/bolsas/nueva`)}>
              <Plus className="w-5 h-5 mr-2" /> Registrar bolsa
            </Button>
          )}
          {canClose && isOpen && (
            <Button className="min-h-[48px]" disabled={pending.length > 0 || !shift.population}
              onClick={() => setIsConfirming(true)}>
              Cerrar turno
            </Button>
          )}
        </div>
      </div>
      <ShiftSteps shift={shift} />

      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}
      {quarteringStatus === 'NO_CONFORME' && (
        <AlertBanner variant="danger" title="Cuarteo no conforme: repesar"
          message="La diferencia supera la tolerancia del 2 %. Revise el cuarteo." />
      )}
      {canClose && isOpen && pending.length > 0 && (
        <p className="m-0 text-sm text-amber-900 flex items-center gap-2">
          <Lock className="w-4 h-4 shrink-0" /> Faltan {plural(pending.length, 'fuente')}: {pending.map((source) => source.code).join(', ')}.
          Registre sus bolsas o márquelas sin generación.
        </p>
      )}

      <div className="grid-left">
        <section className="panel">
          <h3>Población del turno</h3>
          {shift.population ? (
            <>
              <div className="total-band"><span>Total</span><b>{shift.population.total} personas</b></div>
              <p className="m-0 text-sm">
                Población: <strong>{shift.population.total}</strong> personas ({shift.population.students} estudiantes,{' '}
                {shift.population.teachers} docentes, {shift.population.staff} personal)
              </p>
            </>
          ) : (
            <>
              <p className="m-0 text-sm font-medium text-amber-900 flex items-center gap-2">
                <Users className="w-5 h-5 shrink-0" /> Registre primero la población del turno.
              </p>
              {isOpen && (
                <Button className="min-h-[48px]" onClick={() => navigate(`/campo/turno/${shiftId}/poblacion`)}>
                  Registrar población
                </Button>
              )}
            </>
          )}
        </section>

        <section className="panel">
          <div className="panel-head"><h3>Fuentes del turno</h3><small>{formatNumber(shift.totalNetWeight)} kg</small></div>
          <div className="flex flex-col gap-1.5">
            {shift.sources.map((source) => (
              <div key={source.sourceId} className="source-line" data-testid={`source-${source.code}`}>
                <div>
                  <BagIcon kg={source.totalNetWeight} />
                  <span className="min-w-0">
                    <b>{source.code}</b> <span className="text-sm text-[var(--muted)]">{source.name}</span>
                    {source.bagCount > 0 && <small>{plural(source.bagCount, 'bolsa')} · {formatNumber(source.totalNetWeight)} kg</small>}
                  </span>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={source.status} />
                  {isOpen && source.status === 'PENDIENTE' && (
                    <Button variant="outline" size="sm" className="min-h-[48px]" disabled={busySource !== null}
                      isLoading={busySource === source.sourceId} onClick={() => markNoGeneration(source.sourceId, true)}>
                      Marcar sin generación
                    </Button>
                  )}
                  {isOpen && source.status === 'SIN_GENERACION' && (
                    <Button variant="ghost" size="sm" className="min-h-[48px]" disabled={busySource !== null}
                      onClick={() => markNoGeneration(source.sourceId, false)}>
                      Quitar marca
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

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
