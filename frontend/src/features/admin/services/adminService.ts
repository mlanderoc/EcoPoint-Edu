/**
 * API service for administration module (Spec 004, Phase 3).
 *
 * @author craulgomez
 */

import { apiClient } from '@/services/apiClient'
import {
  Crew,
  GenerationSource,
  Institution,
  User,
  WasteCategory,
} from '@/types'

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
    return apiClient.get<User[]>('/api/v1/users')
  },

  createUser: async (data: Omit<User, 'id'>): Promise<User> => {
    return apiClient.post<User>('/api/v1/users', data)
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
}
