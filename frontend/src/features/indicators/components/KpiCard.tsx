/**
 * One indicator card of the dashboard (Spec 006, R-521).
 *
 * @author craulgomez
 */

export interface KpiCardProps {
  label: string
  value: string
  unit?: string
  hint?: string
  href?: string
}

export function KpiCard({ label, value, unit, hint, href }: KpiCardProps) {
  return (
    <div className="kpi" role="group" aria-label={label}>
      <small>{label}</small>
      {href ? <a href={href}>{value}</a> : <b>{value} {unit && <em>{unit}</em>}</b>}
      {hint && <span>{hint}</span>}
    </div>
  )
}
