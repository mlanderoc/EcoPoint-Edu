/**
 * Role-based access control and permission definitions (CA-300 / R-311).
 *
 * @author craulgomez
 */

import { UserRole } from '@/types'

export type PermissionName =
  | 'OPEN_SHIFT'
  | 'CLOSE_SHIFT'
  | 'MANAGE_STUDIES'
  | 'MANAGE_ADMIN'
  | 'RECORD_HAZARDOUS_WASTE'

/**
 * Checks whether a user role has the required permission.
 * Following project access policies (AGENTS.md & Spec 004):
 * - Only DOCENTE can open/close shifts, manage studies, access admin, or record hazardous waste.
 * - ESTUDIANTE_UNIVERSITARIO has field recording and bag sampling capabilities.
 */
export function hasPermission(
  role: UserRole | null | undefined,
  permission: PermissionName
): boolean {
  if (!role) return false

  switch (permission) {
    case 'OPEN_SHIFT':
    case 'CLOSE_SHIFT':
    case 'MANAGE_STUDIES':
    case 'MANAGE_ADMIN':
    case 'RECORD_HAZARDOUS_WASTE':
      return role === 'DOCENTE'
    default:
      return false
  }
}
