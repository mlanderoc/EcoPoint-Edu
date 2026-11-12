import React from 'react'
import { cn } from '@/utils/cn'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
  normalizeDecimal?: boolean
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      label,
      error,
      helperText,
      id,
      inputMode,
      normalizeDecimal = false,
      onChange,
      ...props
    },
    ref
  ) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (normalizeDecimal && (inputMode === 'decimal' || props.type === 'number')) {
        // Normalize comma to period for uniform decimal parsing (R-302)
        const normalized = e.target.value.replace(',', '.')
        e.target.value = normalized
      }
      onChange?.(e)
    }

    return (
      <div className="w-full flex flex-col gap-1.5">
        {label && (
          <label
            htmlFor={inputId}
            className="text-sm font-bold text-[var(--color-text-main)]"
          >
            {label}
          </label>
        )}
        <input
          id={inputId}
          ref={ref}
          inputMode={inputMode}
          onChange={handleChange}
          className={cn(
            'min-h-[48px] px-4 py-2.5 rounded-lg border text-sm md:text-base font-medium transition-colors bg-white',
            'focus:outline-none focus:ring-2 focus:ring-offset-1',
            error
              ? 'border-red-500 text-red-900 focus:ring-red-500 bg-red-50/30'
              : 'border-slate-300 text-[var(--color-text-main)] focus:border-[var(--color-primary)] focus:ring-[var(--color-primary)]',
            props.disabled && 'bg-slate-100 text-slate-400 cursor-not-allowed',
            className
          )}
          {...props}
        />
        {error && (
          <p role="alert" className="text-xs font-semibold text-red-600 mt-0.5">
            {error}
          </p>
        )}
        {!error && helperText && (
          <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
            {helperText}
          </p>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'
