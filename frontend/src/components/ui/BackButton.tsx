/**
 * "Atrás" button of the internal screens. It goes to a fixed screen, not to the
 * browser history, so it never leaves the application when the screen was
 * opened from a direct link (spec 004, R-307; plan D13, decision T10).
 *
 * @author craulgomez
 */

import { ArrowLeft } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export interface BackButtonProps {
  /** Screen to go back to. */
  to: string
  label?: string
  /** Lets a form ask before leaving when it has unsaved changes. */
  onLeave?: (leave: () => void) => void
}

export function BackButton({ to, label = 'Atrás', onLeave }: BackButtonProps) {
  const navigate = useNavigate()
  const leave = () => navigate(to)

  return (
    <button
      type="button"
      onClick={() => (onLeave ? onLeave(leave) : leave())}
      className="self-start inline-flex items-center gap-1.5 min-h-[48px] px-2 -ml-2 rounded-lg text-sm font-bold text-[var(--color-primary)] hover:bg-[var(--color-primary-light)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
    >
      <ArrowLeft className="w-4 h-4" aria-hidden="true" />
      {label}
    </button>
  )
}
