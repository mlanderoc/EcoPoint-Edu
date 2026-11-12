import React from 'react'
import { cn } from '@/utils/cn'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'outline' | 'ghost'
  size?: 'default' | 'sm' | 'lg'
  loading?: boolean
  fullWidth?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'default',
      loading = false,
      fullWidth = false,
      disabled,
      children,
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center font-bold tracking-wide rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none cursor-pointer active:scale-[0.98]'

    // All default and large buttons maintain touch target >= 48px (R-301)
    const sizeStyles = {
      sm: 'min-h-[40px] px-3 py-1.5 text-xs',
      default: 'min-h-[48px] px-5 py-3 text-sm md:text-base',
      lg: 'min-h-[56px] px-6 py-3.5 text-base md:text-lg',
    }

    const variantStyles = {
      primary:
        'bg-[var(--color-primary)] hover:bg-[var(--color-primary-hover)] text-white focus:ring-[var(--color-primary)] shadow-sm',
      secondary:
        'bg-[var(--color-brand-dark)] hover:bg-slate-900 text-white focus:ring-[var(--color-brand-dark)] shadow-sm',
      danger:
        'bg-[var(--color-danger)] hover:bg-red-700 text-white focus:ring-[var(--color-danger)] shadow-sm',
      outline:
        'border-2 border-slate-300 hover:border-slate-400 bg-white text-slate-800 hover:bg-slate-50 focus:ring-slate-400',
      ghost:
        'bg-transparent hover:bg-slate-100 text-slate-800 focus:ring-slate-400',
    }

    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          baseStyles,
          sizeStyles[size],
          variantStyles[variant],
          fullWidth && 'w-full',
          className
        )}
        {...props}
      >
        {loading && (
          <svg
            className="animate-spin -ml-1 mr-2 h-4 w-4 text-current"
            fill="none"
            viewBox="0 0 24 24"
            aria-hidden="true"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        )}
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
