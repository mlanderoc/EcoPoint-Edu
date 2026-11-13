/**
 * Composition of a quartering by group and by subcategory as horizontal bars
 * made with CSS (Spec 005, R-405, decision T3).
 *
 * @author craulgomez
 */

import { WasteGroupBadge } from '@/components/waste'
import { QuarteringResultItem, WasteGroup } from '@/types'
import { formatKg } from '../utils/quarteringMath'

const GROUPS: WasteGroup[] = ['BLANCO', 'VERDE', 'NEGRO', 'SIN_COLOR']
const BAR_COLORS: Record<WasteGroup, string> = {
  BLANCO: 'bg-slate-300',
  VERDE: 'bg-emerald-600',
  NEGRO: 'bg-slate-800',
  SIN_COLOR: 'bg-slate-500',
}

function Bar({ percent, color }: { percent: number; color: string }) {
  return (
    <div className="h-3 w-full rounded-full bg-slate-100 overflow-hidden" aria-hidden="true">
      <div className={`h-full ${color}`} style={{ width: `${Math.min(percent, 100)}%` }} />
    </div>
  )
}

export function CompositionChart({ results, groups }: {
  results: QuarteringResultItem[]
  groups: Record<WasteGroup, number>
}) {
  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-3" data-testid="group-composition">
        {GROUPS.map((group) => (
          <div key={group} className="flex flex-col gap-1">
            <div className="flex items-center justify-between text-sm">
              <WasteGroupBadge group={group} size="sm" />
              <strong>{formatKg(groups[group] ?? 0)} %</strong>
            </div>
            <Bar percent={groups[group] ?? 0} color={BAR_COLORS[group]} />
          </div>
        ))}
      </div>
      <table className="w-full text-sm" data-testid="category-composition">
        <thead>
          <tr className="text-left text-xs text-slate-500">
            <th className="py-1">Subcategoría</th>
            <th className="py-1 text-right">kg</th>
            <th className="py-1 text-right">%</th>
          </tr>
        </thead>
        <tbody>
          {results.map((result) => (
            <tr key={result.categoryId} className="border-t border-slate-100">
              <td className="py-1.5">{result.categoryName}</td>
              <td className="py-1.5 text-right">{formatKg(result.weight)}</td>
              <td className="py-1.5 text-right">{formatKg(result.percentage ?? 0)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
