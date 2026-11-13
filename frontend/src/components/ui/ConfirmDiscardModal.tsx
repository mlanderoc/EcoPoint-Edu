/**
 * Asks before leaving a form with changes that were not saved (spec 004, R-308).
 *
 * @author craulgomez
 */

import { Button } from './Button'
import { Modal } from './Modal'

export interface ConfirmDiscardModalProps {
  open: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDiscardModal({ open, onConfirm, onCancel }: ConfirmDiscardModalProps) {
  return (
    <Modal open={open} onOpenChange={(next) => !next && onCancel()} title="¿Descartar los cambios?"
      description="Lo que escribió todavía no se ha guardado y se perderá.">
      <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
        <Button variant="outline" className="min-h-[48px]" onClick={onCancel}>Seguir editando</Button>
        <Button variant="danger" className="min-h-[48px]" onClick={onConfirm}>Descartar</Button>
      </div>
    </Modal>
  )
}
