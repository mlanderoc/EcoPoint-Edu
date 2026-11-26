/**
 * Home after signing in: the running study as a trail of shifts, the sources
 * of the chosen shift, its composition and what needs attention (D15).
 *
 * @author craulgomez
 */

import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { ClipboardList } from 'lucide-react'
import { AlertBanner, Select } from '@/components/ui'
import { greeting, longDate } from '@/components/layout/Header'
import { useAuth } from '@/features/auth/hooks/useAuth'
import { hasPermission } from '@/features/auth/utils/permissions'
import { useInstitutions } from '@/features/admin/components/InstitutionSelect'
import { fieldService } from '@/features/field/services/fieldService'
import { formatNumber, openBlocker } from '@/features/field/utils/fieldMath'
import { indicatorService } from '@/features/indicators/services/indicatorService'
import { StudyIndicators } from '@/features/indicators/types/indicatorTypes'
import { alertService } from '@/features/quality/services/verificationService'
import { studyService } from '@/features/study/services/studyService'
import { formatDate } from '@/features/study/utils/samplingDays'
import { studyTitle } from '@/features/study/utils/studyStatus'
import { CharacterizationStudy, SamplingShift, ShiftSlot } from '@/types'
import { chosenOrOnly } from '@/utils/choice'
import { BagIcon } from '../components/BagIcon'
import { StudyTrail, slotName, stopState } from '../components/StudyTrail'

const GROUP_COLORS: Record<string, string> = {
  VERDE: 'var(--waste-green)', BLANCO: 'var(--waste-white)', NEGRO: 'var(--waste-black)', SIN_COLOR: 'var(--waste-none)',
}
const GROUP_ORDER = ['VERDE', 'BLANCO', 'NEGRO', 'SIN_COLOR']
const STATE_TEXT = { closed: 'Turno cerrado', current: 'Turno en curso', pending: 'Turno sin abrir' }

function inOrder(slots: ShiftSlot[]): ShiftSlot[] {
  return [...slots].sort((a, b) => a.dayNumber - b.dayNumber || (a.type === 'MANANA' ? -1 : 1))
}

function currentIndex(slots: ShiftSlot[]): number {
  const open = slots.findIndex((slot) => slot.status === 'ABIERTO')
  if (open >= 0) return open
  const next = slots.findIndex((slot) => slot.shiftId === null)
  return next >= 0 ? next : Math.max(0, slots.length - 1)
}

export function HomePage({ now = new Date() }: { now?: Date } = {}) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const canOpen = hasPermission(user?.role, 'MANAGE_SHIFTS')
  const [params, setParams] = useSearchParams()
  const { institutions } = useInstitutions(false)
  const [studies, setStudies] = useState<CharacterizationStudy[] | undefined>(undefined)
  const [slots, setSlots] = useState<ShiftSlot[]>([])
  const [selected, setSelected] = useState(0)
  const [hovered, setHovered] = useState<number | null>(null)
  const [shift, setShift] = useState<SamplingShift | null>(null)
  const [indicators, setIndicators] = useState<StudyIndicators | null>(null)
  const [pendingAlerts, setPendingAlerts] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const chosenId = Number(params.get('estudio'))
  const study = chosenOrOnly(studies, chosenId)
  const studyId = study?.id ?? null
  const slot = slots[selected]
  const shiftId = slot?.shiftId ?? null

  useEffect(() => {
    studyService.fieldStudies()
      .then(setStudies)
      .catch(() => {
        setError('No se pudieron cargar los estudios en ejecución.')
        setStudies([])
      })
    alertService.pendingCount().then(setPendingAlerts).catch(() => setPendingAlerts(0))
  }, [])

  useEffect(() => {
    if (studyId === null) return
    setIndicators(null)
    studyService.shifts(studyId)
      .then((loaded) => {
        const ordered = inOrder(loaded)
        setSlots(ordered)
        setSelected(currentIndex(ordered))
      })
      .catch(() => setError('No se pudieron cargar los turnos del estudio.'))
    indicatorService.get(studyId).then(setIndicators).catch(() => setIndicators(null))
  }, [studyId])

  useEffect(() => {
    if (shiftId === null) {
      setShift(null)
      return
    }
    fieldService.getShift(shiftId).then(setShift).catch(() => setShift(null))
  }, [shiftId])

  const groups = useMemo(() => {
    const all = indicators?.composition.groups ?? []
    return GROUP_ORDER.flatMap((key) => all.filter((group) => group.group === key))
  }, [indicators])
  const classified = groups.reduce((total, group) => total + group.weight, 0)
  const donut = groups.map((group, index) => {
    const from = groups.slice(0, index).reduce((total, item) => total + (item.percentage ?? 0), 0)
    return `${GROUP_COLORS[group.group]} ${from}% ${from + (group.percentage ?? 0)}%`
  }).join(',')

  const institutionName = (id: number) => institutions.find((item) => item.id === id)?.name
  const label = (item: CharacterizationStudy) => {
    const name = institutionName(item.institutionId)
    return name ? `${studyTitle(item)} · ${name}` : studyTitle(item)
  }
  const chooseStudy = (id: number) => {
    const updated = new URLSearchParams(params)
    updated.set('estudio', String(id))
    setParams(updated, { replace: true })
  }

  const state = slot ? stopState(slot) : 'pending'
  const closedShifts = slots.filter((item) => item.status === 'CERRADO').length
  const day = study?.days.find((item) => item.dayNumber === slot?.dayNumber)
  const shiftName = slot?.type === 'MANANA' ? 'Mañana' : 'Tarde'
  const pendingSources = shift?.sources.filter((source) => source.status === 'PENDIENTE') ?? []
  const shiftKg = shift?.totalNetWeight ?? 0
  const orderBlocker = state === 'pending' && slot ? openBlocker(slots, slot.dayNumber, slot.type) : null
  const blocked = state === 'pending' && (!canOpen || orderBlocker !== null)
  const actionText = state === 'closed' ? 'Ver turno'
    : state === 'current' ? 'Continuar turno'
      : !canOpen ? 'Espera la apertura' : orderBlocker ? 'Aún no disponible' : 'Abrir turno'

  const goToShift = () => {
    if (!study || !slot) return
    navigate(shiftId !== null
      ? `/campo/turno/${shiftId}`
      : `/campo?estudio=${study.id}&dia=${slot.dayNumber}&turno=${slot.type}`)
  }

  return (
    <div className="home">
      <div className="home-top">
        <div>
          <h1>{greeting(now)}, {user?.firstName ?? 'de nuevo'}</h1>
          <p className="home-date" data-testid="home-date">{longDate(now)}</p>
        </div>
        {studies && studies.length > 1 && (
          <div className="home-study">
            <Select id="home-study" aria-label="Estudio en ejecución" value={study ? String(study.id) : ''}
              placeholder="Escoja un estudio" icon={<ClipboardList />}
              onChange={(event) => chooseStudy(Number(event.target.value))}
              options={studies.map((item) => ({ value: String(item.id), label: label(item) }))} />
          </div>
        )}
        {study && studies?.length === 1 && <span className="pill">{label(study)}</span>}
      </div>

      {error && <AlertBanner variant="danger" title="No se pudo cargar" message={error} />}
      {studies === undefined && <p className="text-sm text-[var(--color-text-muted)]">Cargando estudio...</p>}
      {!study && studies && studies.length > 1 && (
        <AlertBanner variant="info" title="Escoja un estudio" message="Hay varios estudios en ejecución: elija uno arriba para ver su recorrido." />
      )}
      {studies?.length === 0 && !error && (
        <AlertBanner variant="info" title="No hay un estudio en ejecución"
          message={canOpen
            ? 'Inicia la ejecución de un estudio desde Configuración del estudio.'
            : 'Cuando el docente inicie un estudio de tu cuadrilla, aquí verás su avance.'} />
      )}

      {study && slot && (
        <>
          <section className="trail-hero" aria-label="Recorrido del estudio">
            <div className="trail-head">
              <div className="trail-turn">
                <p className="trail-eyebrow">{STATE_TEXT[state]}</p>
                <h2>Día {slot.dayNumber} · {shiftName}</h2>
                <p className="trail-when">{formatDate(day?.date)}{slot.dayNumber === 1 ? ' · día de descarte' : ''}</p>
                <button className="trail-cta" type="button" disabled={blocked} onClick={goToShift}>{actionText}</button>
                {canOpen && orderBlocker && <p className="trail-why" data-testid="trail-why">{orderBlocker}</p>}
              </div>
              <div className="trail-stats">
                <div><b>{shift?.bagCount ?? 0}</b><span>bolsas del turno</span></div>
                <div><b>{formatNumber(shiftKg)}</b><span>kg del turno</span></div>
                <div><b>{closedShifts} / {slots.length}</b><span>turnos cerrados</span></div>
                <div><b>{study.pendingRecords}</b><span>por verificar</span></div>
              </div>
            </div>
            <StudyTrail slots={slots} selected={selected} onSelect={setSelected} onHover={setHovered} />
            <img className="trail-eco" src="/brand/eco-saluda.webp" alt="" />
            <p className={hovered !== null && slots[hovered] ? 'trail-say on' : 'trail-say'} aria-live="polite"
              data-testid="rokko-says">
              {hovered !== null && slots[hovered] ? slotName(slots[hovered]) : ''}
            </p>
          </section>

          <div className="home-board">
            <section className="home-panel wide">
              <div className="home-panel-head">
                <h3>Fuentes del turno</h3>
                {shift && <small>{shift.sources.length - pendingSources.length} de {shift.sources.length} · {formatNumber(shiftKg)} kg</small>}
              </div>
              {shift ? (
                <div className="home-bags">
                  {shift.sources.map((source) => (
                    <button key={source.sourceId} className="home-bag" type="button" onClick={goToShift}
                      aria-label={`${source.code}: ${source.bagCount > 0 ? `${formatNumber(source.totalNetWeight)} kg` : 'pendiente'}`}>
                      <BagIcon kg={source.totalNetWeight} />
                      <b>{source.code}</b>
                      <span>{source.bagCount > 0 ? `${formatNumber(source.totalNetWeight)} kg`
                        : source.status === 'SIN_GENERACION' ? 'Sin generación' : 'Pendiente'}</span>
                    </button>
                  ))}
                </div>
              ) : <p className="text-sm text-[var(--color-text-muted)]">Este turno aún no se ha abierto.</p>}
            </section>

            <section className="home-panel">
              <div className="home-panel-head"><h3>Composición</h3><small>Cuarteos</small></div>
              {groups.length > 0 && classified > 0 ? (
                <>
                  <div className="home-mix">
                    <div className="home-donut" style={{ background: `conic-gradient(${donut})` }} role="img"
                      aria-label={groups.map((group) => `${group.name} ${formatNumber(group.percentage, 0)} %`).join(', ')}>
                      <div><b>{formatNumber(groups[0].percentage, 0)} %</b><span>{groups[0].name.toLowerCase()}</span></div>
                    </div>
                    <ul className="home-legend">
                      {groups.map((group) => (
                        <li key={group.group}>
                          <i style={{ background: GROUP_COLORS[group.group] }} />
                          {group.name}<b>{formatNumber(group.percentage, 0)} %</b>
                        </li>
                      ))}
                    </ul>
                  </div>
                  <p className="m-0 text-sm text-[var(--color-text-muted)]">Sobre {formatNumber(classified)} kg clasificados</p>
                </>
              ) : <p className="text-sm text-[var(--color-text-muted)]">Aparecerá con el primer cuarteo.</p>}
            </section>

            <section className="home-panel">
              <div className="home-panel-head"><h3>Por atender</h3></div>
              <div className="home-todo">
                <button type="button" onClick={() => navigate('/calidad/verificacion')}>
                  <span className="mark">{study.pendingRecords}</span>
                  <span><b>Registros por verificar</b><small>{studyTitle(study)}</small></span>
                </button>
                <button className={pendingSources.length > 0 ? 'warn' : ''} type="button" onClick={goToShift}>
                  <span className="mark">{pendingSources.length}</span>
                  <span><b>Fuentes por pesar</b><small>{shift ? `Turno ${shift.code}` : 'Turno sin abrir'}</small></span>
                </button>
                <button className={pendingAlerts > 0 ? 'warn' : ''} type="button" onClick={() => navigate('/alertas')}>
                  <span className="mark">{pendingAlerts > 0 ? pendingAlerts : '✓'}</span>
                  <span><b>{pendingAlerts > 0 ? 'Alertas pendientes' : 'Sin alertas pendientes'}</b><small>Calidad del registro</small></span>
                </button>
              </div>
            </section>
          </div>
        </>
      )}
    </div>
  )
}
