/**
 * Rokko, the raccoon mascot, with the steps of the current screen in a small
 * speech bubble (professor's review, point 3). It stays out of the way: the
 * user can fold it into a small button, and the choice is remembered on that
 * device.
 *
 * @author craulgomez
 */

import { useState } from 'react'
import { X } from 'lucide-react'
import { TIPS, TipId } from './tips'

const STORAGE_PREFIX = 'rokko-oculto:'

function readHidden(id: TipId): boolean {
  try {
    return window.localStorage.getItem(STORAGE_PREFIX + id) === '1'
  } catch {
    return false
  }
}

function saveHidden(id: TipId, hidden: boolean) {
  try {
    if (hidden) window.localStorage.setItem(STORAGE_PREFIX + id, '1')
    else window.localStorage.removeItem(STORAGE_PREFIX + id)
  } catch {
    // Without storage the guide simply opens again next time.
  }
}

export interface RokkoTipProps {
  tip: TipId
}

export function RokkoTip({ tip }: RokkoTipProps) {
  const { title, steps } = TIPS[tip]
  const [hidden, setHidden] = useState(() => readHidden(tip))

  const toggle = (next: boolean) => {
    setHidden(next)
    saveHidden(tip, next)
  }

  if (hidden) {
    return (
      <button type="button" className="rokko-mini min-h-[48px]" onClick={() => toggle(false)}
        aria-label={`Ver la guía de Rokko: ${title}`}>
        <span className="rokko-face" aria-hidden="true" />
        ¿Cómo lo hago?
      </button>
    )
  }

  return (
    <aside className="rokko" aria-label={`Guía de Rokko: ${title}`} data-testid={`rokko-${tip}`}>
      <span className="rokko-face" aria-hidden="true" />
      <div className="rokko-bubble">
        <p className="rokko-title"><b>Rokko te guía:</b> {title}</p>
        <ol>
          {steps.map((step) => <li key={step}>{step}</li>)}
        </ol>
      </div>
      <button type="button" className="rokko-hide min-h-[48px] min-w-[48px]" onClick={() => toggle(true)} aria-label="Ocultar la guía de Rokko">
        <X aria-hidden="true" />
      </button>
    </aside>
  )
}
