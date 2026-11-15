/**
 * API service for administration module (Spec 004, Phase 3).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import { PageResponse } from '@/types/api'
import {
  Crew,
  GenerationSource,
  Institution,
  User,
  UserRole,
  UserRow,
  WasteCategory,
} from '@/types'

/** Data to create a user: the API stores the password only as a BCrypt hash. */
export type NewUser = Pick<User, 'firstName' | 'lastName' | 'email' | 'role'> & { password: string }

/** Filters of the users table. */
export interface UserFilters {
  role?: UserRole | ''
  active?: boolean
  text?: string
  page: number
  size: number
}

export const adminService = {
  // Institutions
  getInstitutions: async (): Promise<Institution[]> => {
    return apiClient.get<Institution[]>('/api/v1/institutions')
  },

  createInstitution: async (data: Omit<Institution, 'id'>): Promise<Institution> => {
    return apiClient.post<Institution>('/api/v1/institutions', data)
  },

  updateInstitution: async (id: number, data: Partial<Institution>): Promise<Institution> => {
    return apiClient.put<Institution>(`/api/v1/institutions/${id}`, data)
  },

  // Sources of Generation
  getSources: async (institutionId: number = 1): Promise<GenerationSource[]> => {
    return apiClient.get<GenerationSource[]>(`/api/v1/institutions/${institutionId}/sources`)
  },

  createSource: async (
    institutionId: number,
    data: Omit<GenerationSource, 'id'>
  ): Promise<GenerationSource> => {
    return apiClient.post<GenerationSource>(`/api/v1/institutions/${institutionId}/sources`, data)
  },

  /** Activates or deactivates an institution (RN-006). */
  setInstitutionActive: async (id: number, active: boolean): Promise<Institution> => {
    return apiClient.post<Institution>(`/api/v1/institutions/${id}/${active ? 'activate' : 'deactivate'}`)
  },

  /** Activates or deactivates a source (RN-005). */
  setSourceActive: async (institutionId: number, sourceId: number, active: boolean): Promise<GenerationSource> => {
    return apiClient.post<GenerationSource>(
      `/api/v1/institutions/${institutionId}/sources/${sourceId}/${active ? 'activate' : 'deactivate'}`
    )
  },

  updateSource: async (
    institutionId: number,
    sourceId: number,
    data: Partial<GenerationSource>
  ): Promise<GenerationSource> => {
    return apiClient.put<GenerationSource>(
      `/api/v1/institutions/${institutionId}/sources/${sourceId}`,
      data
    )
  },

  // Users
  getUsers: async (): Promise<User[]> => {
    const page = await apiClient.get<PageResponse<User>>('/api/v1/users', { params: { size: 200 } })
    return page.items
  },

  createUser: async (data: NewUser): Promise<User> => {
    return apiClient.post<User>('/api/v1/users', data)
  },

  /** One page of the users table, filtered on the server (R-322; plan D13, decision T14). */
  listUsers: async (filters: UserFilters): Promise<PageResponse<UserRow>> => {
    return apiClient.get<PageResponse<UserRow>>('/api/v1/users', {
      params: {
        role: filters.role || undefined,
        active: filters.active,
        q: filters.text?.trim() || undefined,
        page: filters.page,
        size: filters.size,
      },
    })
  },

  /** Activates or deactivates a student; a teacher is never deactivated (RN-087). */
  setUserActive: async (id: number, active: boolean): Promise<User> => {
    return apiClient.post<User>(`/api/v1/users/${id}/${active ? 'activate' : 'deactivate'}`)
  },

  // Crews
  getCrews: async (): Promise<Crew[]> => {
    return apiClient.get<Crew[]>('/api/v1/crews')
  },

  createCrew: async (data: Omit<Crew, 'id'>): Promise<Crew> => {
    return apiClient.post<Crew>('/api/v1/crews', data)
  },

  // Waste Categories
  getCategories: async (): Promise<WasteCategory[]> => {
    return apiClient.get<WasteCategory[]>('/api/v1/waste-categories')
  },

  createCategory: async (data: Omit<WasteCategory, 'id'>): Promise<WasteCategory> => {
    return apiClient.post<WasteCategory>('/api/v1/waste-categories', data)
  },

  /** Activates or deactivates a subcategory (RN-043). */
  setCategoryActive: async (id: number, active: boolean): Promise<WasteCategory> => {
    return apiClient.post<WasteCategory>(`/api/v1/waste-categories/${id}/${active ? 'activate' : 'deactivate'}`)
  },
}
