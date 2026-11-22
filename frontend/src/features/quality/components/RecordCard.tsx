/**
 * A verifiable record with its typed values and double-entry state
 * (Spec 005, R-421, R-423).
 *
 * @author craulgomez
 */

import { Badge } from './Badge'
import { formatNumber } from '@/features/field/utils/fieldMath'
import { VerifiableRecord, VerificationStatus } from '@/types'

export const STATUS_LABELS: Record<VerificationStatus, string> = {
  PENDIENTE: 'Pendiente',
  VERIFICADO: 'Verificado',
  CORREGIDO: 'Corregido',
}

/** Formats a field value: numbers with decimal comma, the rest as text. */
export function formatValue(value: number | string | null): string {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'number') {
    return Number.isInteger(value) ? String(value) : formatNumber(value, value < 1 ? 3 : 2)
  }
  return value
}

export function RecordCard({ record, children }: { record: VerifiableRecord; children?: React.ReactNode }) {
  const status = record.verification.status
  return (
    <div className="record-card" data-testid={`record-${record.recordType}-${record.recordId}`}>
      <div className="flex items-center justify-between gap-2">
        <p className="m-0 font-[family-name:var(--display)] font-black">{record.title}</p>
        <Badge tone={status === 'PENDIENTE' ? 'warning' : 'success'}>{STATUS_LABELS[status]}</Badge>
      </div>
      <dl>
        {record.fields.map((field) => (
          <div key={field.field}>
            <dt>{field.label}</dt>
            <dd>{formatValue(field.value)}</dd>
          </div>
        ))}
      </dl>
      {children}
    </div>
  )
}
