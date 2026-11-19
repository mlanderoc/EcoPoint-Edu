/**
 * Filter of the administration lists: active, inactive or all. By default only
 * the active items are shown; the choice lives in the URL (?estado=) so it
 * survives going back (spec 004, R-325; plan D13, decision T9). It has no eye
 * toggle: the three options already do that job (D14).
 *
 * @author craulgomez
 */

import { useSearchParams } from 'react-router-dom'
import { cn } from '@/utils/cn'

export type ActiveFilterValue = 'activos' | 'inactivos' | 'todos'

const OPTIONS: Array<{ value: ActiveFilterValue; label: string }> = [
  { value: 'activos', label: 'Activos' },
  { value: 'inactivos', label: 'Inactivos' },
  { value: 'todos', label: 'Todos' },
]

/** Reads and changes the filter kept in the URL. */
export function useActiveFilter(): [ActiveFilterValue, (value: ActiveFilterValue) => void] {
  const [params, setParams] = useSearchParams()
  const current = params.get('estado')
  const value: ActiveFilterValue = current === 'inactivos' || current === 'todos' ? current : 'activos'
  const change = (next: ActiveFilterValue) => {
    const updated = new URLSearchParams(params)
    if (next === 'activos') updated.delete('estado')
    else updated.set('estado', next)
    setParams(updated, { replace: true })
  }
  return [value, change]
}

/** True if an item with that state is shown with the filter. */
export function matchesActiveFilter(active: boolean, filter: ActiveFilterValue): boolean {
  return filter === 'todos' || (filter === 'activos') === active
}

export interface ActiveFilterProps {
  value: ActiveFilterValue
  onChange: (value: ActiveFilterValue) => void
  /** How many inactive items exist, shown next to the option. */
  inactiveCount?: number
}

export function ActiveFilter({ value, onChange, inactiveCount }: ActiveFilterProps) {
  return (
    <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Mostrar por estado">
      <div className="inline-flex rounded-lg border border-slate-200 bg-white p-0.5">
        {OPTIONS.map((option) => (
          <button
            key={option.value}
            type="button"
            aria-pressed={value === option.value}
            onClick={() => onChange(option.value)}
            className={cn(
              'min-h-[44px] px-3 rounded-md text-sm font-semibold transition-colors',
              value === option.value
                ? 'bg-[var(--color-primary)] text-white'
                : 'text-slate-600 hover:bg-slate-100'
            )}
          >
            {option.label}
            {option.value === 'inactivos' && inactiveCount !== undefined ? ` (${inactiveCount})` : ''}
          </button>
        ))}
      </div>
    </div>
  )
}
