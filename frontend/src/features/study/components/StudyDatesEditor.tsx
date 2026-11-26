/**
 * The 8 dates of a study. Changing any day recalculates the later days as the
 * following business days, skipping weekends and Colombian holidays (spec 004,
 * R-330, R-333, R-334; plan D13, decision T2). Used when creating a study and
 * in the detail of a planned study.
 *
 * @author craulgomez
 */

import { AlertBanner, Input } from '@/components/ui'
import { formatDate, nonBusinessReason, pastDateErrors, recalculateFrom, todayIso } from '../utils/samplingDays'

export interface StudyDatesEditorProps {
  dates: string[]
  onChange: (dates: string[]) => void
  disabled?: boolean
  /** Dates already saved; they may stay in the past (professor's review, point 5). */
  saved?: string[]
  /** Current moment; tests pass a fixed one. */
  now?: Date
}

export function StudyDatesEditor({ dates, onChange, disabled = false, saved = [], now }: StudyDatesEditorProps) {
  const today = todayIso(now)
  const pastErrors = pastDateErrors(dates, saved, today)
  const warnings = dates
    .map((date, index) => {
      const reason = date ? nonBusinessReason(date) : null
      return reason ? `Día ${index + 1} (${reason})` : null
    })
    .filter((warning): warning is string => warning !== null)

  return (
    <div className="flex flex-col gap-4">
      <AlertBanner
        variant="info"
        title="Día 1: Día de descarte: no entra en los cálculos"
        message="Los residuos del día 1 se acumularon antes del estudio; se pesan pero no cuentan para la línea base."
      />
      <p className="text-sm text-[var(--color-text-muted)]">
        Al elegir o cambiar un día, los días siguientes se recalculan con los días hábiles que siguen: sin sábados,
        domingos ni festivos de Colombia. Solo se aceptan fechas desde hoy en adelante.
      </p>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {dates.map((date, index) => (
          <Input
            key={index}
            id={`study-day-${index + 1}`}
            type="date"
            label={index === 0 ? 'Día 1 (descarte)' : `Día ${index + 1}`}
            value={date}
            min={date && date === saved[index] && date < today ? undefined : today}
            onChange={(event) => onChange(recalculateFrom(dates, index, event.target.value))}
            helperText={date ? formatDate(date) : undefined}
            error={pastErrors[index]}
            disabled={disabled}
            required={index === 0}
          />
        ))}
      </div>
      {warnings.length > 0 && (
        <AlertBanner
          variant="warning"
          message={`No son días hábiles: ${warnings.join(', ')}. Revise que haya clases esos días.`}
        />
      )}
    </div>
  )
}
