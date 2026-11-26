/**
 * Drop-down list of the design system. It always shows the arrow that tells
 * there are more values, and an optional "Escoja..." message instead of a value
 * chosen by default (professor's review, point 7).
 *
 * @author craulgomez
 */

import React from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface SelectOption {
  value: string | number
  label: string
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options?: SelectOption[]
  /** First, empty option shown while nothing is chosen, such as "Escoja un estudio". */
  placeholder?: string
  /** Small icon at the left of the value. */
  icon?: React.ReactNode
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, id, options = [], children, placeholder, icon, ...props }, ref) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)
    const isEmpty = props.value === '' || props.value === undefined

    return (
      <div className="w-full flex flex-col gap-1.5">
        {label && (
          <label
            htmlFor={selectId}
            className="text-sm font-bold text-[var(--color-text-main)]"
          >
            {label}
          </label>
        )}
        <div className={cn('select-box', error && 'is-error', props.disabled && 'is-disabled', icon ? 'has-icon' : undefined)}>
          {icon && <span className="select-icon" aria-hidden="true">{icon}</span>}
          <select
            id={selectId}
            ref={ref}
            className={cn(
              'select-field min-h-[48px] text-sm md:text-base font-semibold appearance-none',
              isEmpty && placeholder ? 'is-placeholder' : undefined,
              error ? 'text-red-900' : 'text-[var(--color-text-main)]',
              props.disabled && 'cursor-not-allowed',
              className
            )}
            aria-invalid={error ? true : undefined}
            {...props}
          >
            {placeholder && <option value="" disabled>{placeholder}</option>}
            {options.length > 0
              ? options.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))
              : children}
          </select>
          <ChevronDown className="select-arrow" aria-hidden="true" />
        </div>
        {error && (
          <p role="alert" className="text-xs font-semibold text-red-600 mt-0.5">
            {error}
          </p>
        )}
      </div>
    )
  }
)

Select.displayName = 'Select'
