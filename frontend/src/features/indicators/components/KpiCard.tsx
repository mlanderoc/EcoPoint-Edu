/**
 * One indicator card of the dashboard (Spec 006, R-521).
 *
 * @author craulgomez
 */

import { Card, CardContent } from '@/components/ui'

export interface KpiCardProps {
  label: string
  value: string
  unit?: string
  hint?: string
  href?: string
}

export function KpiCard({ label, value, unit, hint, href }: KpiCardProps) {
  return (
    <Card>
      <CardContent className="flex flex-col gap-1 py-4" aria-label={label}>
        <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
        {href ? (
          <a href={href} className="text-xl font-black text-[var(--color-primary)] underline min-h-[44px] flex items-center">
            {value}
          </a>
        ) : (
          <span className="text-2xl font-black text-[var(--color-brand-dark)]">
            {value} {unit && <span className="text-sm font-semibold text-slate-500">{unit}</span>}
          </span>
        )}
        {hint && <span className="text-xs text-slate-500">{hint}</span>}
      </CardContent>
    </Card>
  )
}
