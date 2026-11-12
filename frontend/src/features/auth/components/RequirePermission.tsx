/**
 * Conditional rendering component based on user permission (CA-300 / R-311).
 *
 * @author craulgomez
 */

import React from 'react'
import { useAuth } from '../hooks/useAuth'
import { hasPermission, PermissionName } from '../utils/permissions'

export interface RequirePermissionProps {
  permission: PermissionName
  fallback?: React.ReactNode
  children: React.ReactNode
}

export function RequirePermission({
  permission,
  fallback = null,
  children,
}: RequirePermissionProps) {
  const { role } = useAuth()
  const allowed = hasPermission(role, permission)

  if (!allowed) {
    return <>{fallback}</>
  }

  return <>{children}</>
}
