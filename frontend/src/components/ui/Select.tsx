import React from 'react'
import { cn } from '@/utils/cn'

export interface SelectOption {
  value: string | number
  label: string
}

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options?: SelectOption[]
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, id, options = [], children, ...props }, ref) => {
    const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined)

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
        <select
          id={selectId}
          ref={ref}
          className={cn(
            'min-h-[48px] px-4 py-2.5 rounded-lg border text-sm md:text-base font-medium transition-colors bg-white appearance-none',
            'focus:outline-none focus:ring-2 focus:ring-offset-1',
            error
              ? 'border-red-500 text-red-900 focus:ring-red-500 bg-red-50/30'
              : 'border-slate-300 text-[var(--color-text-main)] focus:border-[var(--color-primary)] focus:ring-[var(--color-primary)]',
            props.disabled && 'bg-slate-100 text-slate-400 cursor-not-allowed',
            className
          )}
          {...props}
        >
          {options.length > 0
            ? options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))
            : children}
        </select>
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
