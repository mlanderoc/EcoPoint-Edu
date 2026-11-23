/**
 * The four steps of a shift, with the progress of each one (D15).
 *
 * @author craulgomez
 */

import { Link } from 'react-router-dom'
import { SamplingShift } from '@/types'

export type ShiftStep = 'poblacion' | 'bolsas' | 'cuarteo' | 'densidad'

export function ShiftSteps({ shift, current }: { shift: SamplingShift; current?: ShiftStep }) {
  const base = `/campo/turno/${shift.id}`
  const covered = shift.sources.filter((source) => source.status !== 'PENDIENTE').length
  const canWeigh = shift.status === 'ABIERTO' && Boolean(shift.population)
  const hasBags = Boolean(shift.population) && shift.bagCount > 0
  const steps = [
    { id: 'poblacion', name: 'Población', note: shift.population ? `${shift.population.total} personas` : 'Pendiente',
      done: Boolean(shift.population), href: shift.population ? base : `${base}/poblacion`, enabled: true },
    { id: 'bolsas', name: 'Bolsas', note: `${covered} de ${shift.sources.length} fuentes`,
      done: shift.sources.length > 0 && covered === shift.sources.length, href: `${base}/bolsas/nueva`, enabled: canWeigh },
    { id: 'cuarteo', name: 'Cuarteo', note: shift.hasQuartering ? 'Registrado' : 'Pendiente',
      done: shift.hasQuartering, href: `${base}/cuarteo`, enabled: hasBags },
    { id: 'densidad', name: 'Densidad', note: shift.hasDensity ? 'Registrada' : 'Pendiente',
      done: shift.hasDensity, href: `${base}/densidad`, enabled: hasBags },
  ]
  return (
    <nav className="shift-steps" aria-label="Pasos del turno">
      {steps.map((step, index) => {
        const body = (
          <>
            <i>{step.done ? '✓' : index + 1}</i>
            <span><b>{step.name}</b><small>{step.note}</small></span>
          </>
        )
        const className = `shift-step ${step.done ? 'done' : ''}`
        const ariaCurrent = current === step.id ? 'step' as const : undefined
        return step.enabled
          ? <Link key={step.id} className={className} to={step.href} aria-current={ariaCurrent}>{body}</Link>
          : <span key={step.id} className={className} aria-disabled="true" aria-current={ariaCurrent}>{body}</span>
      })}
    </nav>
  )
}
