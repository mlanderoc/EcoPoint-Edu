/**
 * Hook to evaluate whether the current authenticated user has a specific permission.
 *
 * @author craulgomez
 */

import { useAuth } from './useAuth'
import { hasPermission, PermissionName } from '../utils/permissions'

export function usePermission(permission: PermissionName): boolean {
  const { role } = useAuth()
  return hasPermission(role, permission)
}
