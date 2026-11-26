/**
 * The eight sampling days drawn as a trail with one stop per shift (D15).
 *
 * @author craulgomez
 */

import { ShiftSlot } from '@/types'

const point = (index: number) => ({ x: 34 + index * (932 / 15), y: 70 + 34 * Math.sin(index * 0.78) })

function curve(count: number): string {
  let path = ''
  for (let index = 0; index < count; index++) {
    const current = point(index)
    if (index === 0) {
      path = `M${current.x} ${current.y}`
      continue
    }
    const previous = point(index - 1)
    const middle = (previous.x + current.x) / 2
    path += ` C${middle} ${previous.y} ${middle} ${current.y} ${current.x} ${current.y}`
  }
  return path
}

export type StopState = 'closed' | 'current' | 'pending'

export function stopState(slot: ShiftSlot): StopState {
  if (slot.status === 'CERRADO') return 'closed'
  return slot.status === 'ABIERTO' ? 'current' : 'pending'
}

export interface StudyTrailProps {
  slots: ShiftSlot[]
  selected: number
  onSelect: (index: number) => void
  /** Stop under the pointer or the keyboard focus, so Rokko can say its day and shift (point 6). */
  onHover?: (index: number | null) => void
}

/** "Día 3, Turno Mañana". */
export function slotName(slot: ShiftSlot): string {
  return `Día ${slot.dayNumber}, Turno ${slot.type === 'MANANA' ? 'Mañana' : 'Tarde'}`
}

export function StudyTrail({ slots, selected, onSelect, onHover }: StudyTrailProps) {
  const reached = slots.reduce((last, slot, index) => (slot.shiftId !== null ? index : last), -1)
  return (
    <div className="trail-scroll">
      <div className="trail">
        <svg viewBox="0 0 1000 176" preserveAspectRatio="none" aria-hidden="true">
          <path className="trail-road" d={curve(slots.length)} />
          {reached >= 0 && <path className="trail-done" d={curve(reached + 1)} />}
        </svg>
        {slots.map((slot, index) => {
          const position = point(index)
          const shiftName = slot.type === 'MANANA' ? 'Mañana' : 'Tarde'
          return (
            <button
              key={`${slot.dayNumber}-${slot.type}`}
              type="button"
              className={`trail-stop ${stopState(slot)}`}
              title={slotName(slot)}
              onMouseEnter={() => onHover?.(index)}
              onMouseLeave={() => onHover?.(null)}
              onFocus={() => onHover?.(index)}
              onBlur={() => onHover?.(null)}
              style={{ left: `${position.x / 10}%`, top: `${(position.y / 176) * 100}%` }}
              aria-label={`Día ${slot.dayNumber}, ${shiftName}`}
              aria-pressed={index === selected}
              onClick={() => onSelect(index)}
            >
              {slot.type === 'MANANA' ? 'M' : 'T'}
            </button>
          )
        })}
        {slots.filter((slot) => slot.type === 'MANANA').map((slot) => {
          const position = point((slot.dayNumber - 1) * 2)
          return (
            <div key={slot.dayNumber} className="trail-day" style={{ left: `${(position.x + 466 / 15) / 10}%` }}>
              <b>Día {slot.dayNumber}</b>
              {slot.dayNumber === 1 && <><br />descarte</>}
            </div>
          )
        })}
      </div>
    </div>
  )
}
