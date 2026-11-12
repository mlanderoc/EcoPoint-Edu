/**
 * Route guard restricting access to specific user roles (CA-300 / R-311).
 * Redirects unauthorized users to fallbackPath (default: /campo) with accessDenied state.
 *
 * @author craulgomez
 */

import React from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { UserRole } from '@/types'
import { useAuth } from '../hooks/useAuth'

export interface RequireRoleProps {
  allowedRoles: UserRole[]
  fallbackPath?: string
  children?: React.ReactNode
}

export function RequireRole({
  allowedRoles,
  fallbackPath = '/campo',
  children,
}: RequireRoleProps) {
  const { role } = useAuth()
  const location = useLocation()

  if (!role || !allowedRoles.includes(role)) {
    return (
      <Navigate
        to={fallbackPath}
        state={{ accessDenied: true, attemptedPath: location.pathname }}
        replace
      />
    )
  }

  return children ? <>{children}</> : <Outlet />
}
