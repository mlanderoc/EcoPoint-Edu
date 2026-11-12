/**
 * Modal dialog atomic component wrapping Radix UI Dialog.
 *
 * @author craulgomez
 */

import React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { X } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface ModalProps {
  open?: boolean
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
  onClose?: () => void
  title: string
  description?: string
  children: React.ReactNode
  className?: string
}

export function Modal({
  open,
  isOpen,
  onOpenChange,
  onClose,
  title,
  description,
  children,
  className,
}: ModalProps) {
  const isModalOpen = open !== undefined ? open : Boolean(isOpen)

  const handleOpenChange = (nextOpen: boolean) => {
    onOpenChange?.(nextOpen)
    if (!nextOpen && onClose) {
      onClose()
    }
  }

  return (
    <DialogPrimitive.Root open={isModalOpen} onOpenChange={handleOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs transition-opacity animate-in fade-in" />
        <DialogPrimitive.Content
          className={cn(
            'fixed left-[50%] top-[50%] z-50 max-h-[90vh] w-[92vw] max-w-lg translate-x-[-50%] translate-y-[-50%] overflow-y-auto rounded-2xl bg-white p-5 md:p-6 shadow-2xl duration-200 animate-in fade-in-0 zoom-in-95',
            className
          )}
        >
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <DialogPrimitive.Title className="text-lg md:text-xl font-bold text-[var(--color-text-main)]">
              {title}
            </DialogPrimitive.Title>
            <DialogPrimitive.Close className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition">
              <X className="w-5 h-5" />
              <span className="sr-only">Cerrar</span>
            </DialogPrimitive.Close>
          </div>
          {description && (
            <DialogPrimitive.Description className="text-xs md:text-sm text-[var(--color-text-muted)] mt-2">
              {description}
            </DialogPrimitive.Description>
          )}
          <div className="mt-4">{children}</div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  )
}
