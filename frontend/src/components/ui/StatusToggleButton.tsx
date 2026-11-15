/**
 * "Activar" or "Desactivar" button of an administration row (spec 004, R-320,
 * R-321, R-322, R-324).
 *
 * @author craulgomez
 */

import { Power, PowerOff } from 'lucide-react'
import { Button } from './Button'

export interface StatusToggleButtonProps {
  active: boolean
  /** Name of the item, read by screen readers, e.g. "AU1". */
  itemName: string
  onToggle: () => void
  disabled?: boolean
}

export function StatusToggleButton({ active, itemName, onToggle, disabled = false }: StatusToggleButtonProps) {
  const action = active ? 'Desactivar' : 'Activar'
  return (
    <Button
      type="button"
      variant="outline"
      className="min-h-[48px] px-3 text-xs"
      onClick={onToggle}
      disabled={disabled}
      aria-label={`${action} ${itemName}`}
    >
      {active ? <PowerOff className="w-4 h-4 mr-1.5" aria-hidden="true" /> : <Power className="w-4 h-4 mr-1.5" aria-hidden="true" />}
      {action}
    </Button>
  )
}
