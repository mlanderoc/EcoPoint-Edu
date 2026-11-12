/**
 * Route guard requiring an authenticated session (R-310).
 * Redirects unauthenticated users to /login.
 *
 * @author craulgomez
 */

import React from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export interface RequireAuthProps {
  children?: React.ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return (
      <div
        role="status"
        aria-live="polite"
        className="min-h-screen flex items-center justify-center p-6 bg-[var(--color-bg-app)]"
      >
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-slate-200 border-t-[var(--color-primary)] rounded-full animate-spin" />
          <p className="text-sm font-semibold text-[var(--color-text-muted)]">
            Verificando sesión...
          </p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children ? <>{children}</> : <Outlet />
}
