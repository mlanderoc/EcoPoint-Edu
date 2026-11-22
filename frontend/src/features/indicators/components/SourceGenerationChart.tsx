/**
 * Average daily generation by source, highest first (Spec 006, R-524).
 *
 * @author craulgomez
 */

import { SourceGeneration } from '../types/indicatorTypes'
import { formatNumber } from '../utils/format'

export function SourceGenerationChart({ sources }: { sources: SourceGeneration[] }) {
  const max = Math.max(1, ...sources.map((source) => source.averageDailyGeneration ?? 0))
  return (
    <section className="panel">
      <div className="panel-head"><h3>Generación por fuente</h3><small>kg/día</small></div>
      {sources.length === 0 && <p className="m-0 text-sm text-slate-500">Aún no hay bolsas en los días 2 a 8.</p>}
      <ol className="source-bars" data-testid="source-ranking">
        {sources.map((source) => (
          <li key={source.sourceId} className="h-bar" title={source.name}>
            <span className="code">{source.code}<span className="sr-only"> · {source.name}</span></span>
            <div className="track"><i style={{ width: `${((source.averageDailyGeneration ?? 0) / max) * 100}%` }} /></div>
            <b>{formatNumber(source.averageDailyGeneration)}<span className="sr-only"> kg/día</span></b>
          </li>
        ))}
      </ol>
    </section>
  )
}
