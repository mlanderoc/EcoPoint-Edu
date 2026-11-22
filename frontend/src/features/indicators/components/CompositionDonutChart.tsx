/**
 * Composition by the four groups of Resolution 2184 of 2019 as a donut
 * (Spec 006, R-523).
 *
 * @author craulgomez
 */

import { Composition } from '../types/indicatorTypes'
import { GROUP_COLORS, formatNumber } from '../utils/format'

const ORDER = ['VERDE', 'BLANCO', 'NEGRO', 'SIN_COLOR']

export function CompositionDonutChart({ composition, recoverable }: { composition: Composition; recoverable?: number | null }) {
  const groups = ORDER.flatMap((key) => composition.groups.filter((group) => group.group === key))
  const hasData = groups.some((group) => (group.percentage ?? 0) > 0)
  const stops = groups.map((group, index) => {
    const from = groups.slice(0, index).reduce((total, item) => total + (item.percentage ?? 0), 0)
    return `${GROUP_COLORS[group.group]} ${from}% ${from + (group.percentage ?? 0)}%`
  }).join(',')
  return (
    <section className="panel">
      <h3>Composición</h3>
      {!hasData && <p className="m-0 text-sm text-slate-500">Aún no hay cuarteos válidos para calcular la composición.</p>}
      <div className={hasData ? 'mix' : ''}>
        {hasData && (
          <div className="donut" style={{ background: `conic-gradient(${stops})` }} role="img"
            aria-label={groups.map((group) => `${group.name} ${formatNumber(group.percentage, 0)} %`).join(', ')}>
            <div><b>{formatNumber(recoverable, 0)} %</b><span>aprovechable</span></div>
          </div>
        )}
        <ul className="legend" data-testid="group-shares">
          {groups.map((group) => (
            <li key={group.group}>
              <i style={{ background: GROUP_COLORS[group.group] }} aria-hidden="true" />
              <span>{group.name}</span><b>{formatNumber(group.percentage)} %</b>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
