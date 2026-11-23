/**
 * Generation of days 1 to 8, with day 1 greyed out as the discard day
 * (Spec 006, R-522).
 *
 * @author craulgomez
 */

import { DailyGeneration } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

export function DailyGenerationChart({ days }: { days: DailyGeneration[] }) {
  const data = days.map((day) => ({ ...day, name: `Día ${day.dayNumber}` }))
  const max = Math.max(1, ...data.map((day) => day.generation ?? 0))
  return (
    <section className="panel">
      <div className="panel-head"><h3>Generación por día</h3><small>kg</small></div>
      <div className="bars" aria-hidden="true">
        {data.map((day) => {
          const height = ((day.generation ?? 0) / max) * 86
          return (
            <div key={day.dayNumber} className={day.excluded ? 'bar off' : 'bar'}>
              <span style={{ bottom: `calc(${height}% + 3px)` }}>{formatNumber(day.generation, 0)}</span>
              <i style={{ height: `${height}%` }} />
            </div>
          )
        })}
      </div>
      <div className="bar-labels" aria-hidden="true">
        {data.map((day) => <span key={day.dayNumber}>Día {day.dayNumber}{day.excluded ? '*' : ''}</span>)}
      </div>
      <small className="text-[var(--muted)]">* Día de descarte: se muestra pero no entra en ningún cálculo.</small>
      <ul className="sr-only" data-testid="daily-generation">
        {data.map((day) => (
          <li key={day.dayNumber}>{day.name}{day.excluded ? ' (descarte)' : ''}: {formatNumber(day.generation)} kg</li>
        ))}
      </ul>
    </section>
  )
}
