/**
 * Protects a form with unsaved changes (spec 004, R-308; plan D13, decision
 * T11): leaving through "Atrás" or "Descartar" asks "¿Descartar los cambios?",
 * and closing the tab shows the browser warning.
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { ConfirmDiscardModal } from '@/components/ui/ConfirmDiscardModal'

export function useUnsavedChanges(isDirty: boolean) {
  const [pendingLeave, setPendingLeave] = useState<(() => void) | null>(null)

  useEffect(() => {
    if (!isDirty) return undefined
    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [isDirty])

  /** Runs {@code leave} at once, or after confirming when there are unsaved changes. */
  const confirmLeave = useCallback((leave: () => void) => {
    if (isDirty) {
      setPendingLeave(() => leave)
    } else {
      leave()
    }
  }, [isDirty])

  const discardDialog = (
    <ConfirmDiscardModal
      open={pendingLeave !== null}
      onCancel={() => setPendingLeave(null)}
      onConfirm={() => {
        const leave = pendingLeave
        setPendingLeave(null)
        leave?.()
      }}
    />
  )

  return { confirmLeave, discardDialog }
}
