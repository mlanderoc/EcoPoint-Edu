/**
 * Animated on/off switch of an administration row (spec 004, R-320, R-321,
 * R-322, R-324; D15).
 *
 * @author craulgomez
 */

export interface StatusToggleButtonProps {
  active: boolean
  /** Name of the item, read by screen readers, e.g. "AU1". */
  itemName: string
  onToggle: () => void
  disabled?: boolean
}

export function StatusToggleButton({ active, itemName, onToggle, disabled = false }: StatusToggleButtonProps) {
  return (
    <button
      type="button"
      role="switch"
      className="switch"
      aria-checked={active}
      aria-label={`${active ? 'Desactivar' : 'Activar'} ${itemName}`}
      onClick={onToggle}
      disabled={disabled}
    />
  )
}
