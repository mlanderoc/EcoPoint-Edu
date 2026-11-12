import React from 'react'
import { AlertCircle, AlertTriangle, CheckCircle, Info } from 'lucide-react'
import { cn } from '@/utils/cn'

export interface AlertBannerProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'warning' | 'danger' | 'info' | 'success'
  title?: string
  children: React.ReactNode
}

export function AlertBanner({
  variant = 'info',
  title,
  children,
  className,
  ...props
}: AlertBannerProps) {
  const icons = {
    warning: <AlertTriangle className="w-5 h-5 text-[var(--color-warning)] shrink-0 mt-0.5" />,
    danger: <AlertCircle className="w-5 h-5 text-[var(--color-danger)] shrink-0 mt-0.5" />,
    info: <Info className="w-5 h-5 text-[var(--color-edu-blue)] shrink-0 mt-0.5" />,
    success: <CheckCircle className="w-5 h-5 text-[var(--color-success)] shrink-0 mt-0.5" />,
  }

  const styles = {
    warning: 'bg-[var(--color-warning-bg)] border-amber-300 text-amber-950',
    danger: 'bg-[var(--color-danger-bg)] border-red-300 text-red-950',
    info: 'bg-blue-50 border-blue-200 text-blue-950',
    success: 'bg-emerald-50 border-emerald-200 text-emerald-950',
  }

  return (
    <div
      role="alert"
      className={cn(
        'rounded-xl border p-4 flex items-start gap-3 text-xs md:text-sm font-medium shadow-xs',
        styles[variant],
        className
      )}
      {...props}
    >
      {icons[variant]}
      <div className="flex-1">
        {title && <p className="font-bold mb-0.5">{title}</p>}
        <div>{children}</div>
      </div>
    </div>
  )
}
