/**
 * Role-based access control and permission matrix mirroring RN-081 (CA-300 / R-311 / RN-081).
 * Exact mirror of backend com.ecopoint.model.Permission and UserRole.
 *
 * @author craulgomez
 */

import { UserRole } from '@/types'

export type Permission =
  | 'MANAGE_ADMINISTRATION'
  | 'MANAGE_STUDIES'
  | 'MANAGE_SHIFTS'
  | 'REGISTER_POPULATION'
  | 'REGISTER_BAGS'
  | 'REGISTER_QUARTERING'
  | 'REGISTER_HAZARDOUS_WASTE'
  | 'VERIFY_RECORDS'
  | 'ANNUL_QUARTERING'
  | 'ATTEND_ALERTS'
  | 'VIEW_INDICATORS'

export type PermissionName = Permission

export const ALL_PERMISSIONS: Permission[] = [
  'MANAGE_ADMINISTRATION',
  'MANAGE_STUDIES',
  'MANAGE_SHIFTS',
  'REGISTER_POPULATION',
  'REGISTER_BAGS',
  'REGISTER_QUARTERING',
  'REGISTER_HAZARDOUS_WASTE',
  'VERIFY_RECORDS',
  'ANNUL_QUARTERING',
  'ATTEND_ALERTS',
  'VIEW_INDICATORS',
]

/**
 * Checks whether a user role has the required permission per the RN-081 matrix.
 * - DOCENTE has all 11 permissions.
 * - ESTUDIANTE_UNIVERSITARIO has 8 permissions (cannot manage administration, studies, or shifts;
 *   can register population, bags, quartering, hazardous waste, verify records, annul quartering,
 *   attend alerts, and view indicators per RN-081 and decision D9).
 */
export function hasPermission(
  role: UserRole | null | undefined,
  permission: Permission
): boolean {
  if (!role || !permission) return false

  if (role === 'DOCENTE') {
    return true
  }

  if (role === 'ESTUDIANTE_UNIVERSITARIO') {
    switch (permission) {
      case 'MANAGE_ADMINISTRATION':
      case 'MANAGE_STUDIES':
      case 'MANAGE_SHIFTS':
        return false
      case 'REGISTER_POPULATION':
      case 'REGISTER_BAGS':
      case 'REGISTER_QUARTERING':
      case 'REGISTER_HAZARDOUS_WASTE':
      case 'VERIFY_RECORDS':
      case 'ANNUL_QUARTERING':
      case 'ATTEND_ALERTS':
      case 'VIEW_INDICATORS':
        return true
      default:
        return false
    }
  }

  return false
}
