/**
 * Characterization limits of the institution a study belongs to. The field
 * screens use them, not the ones of the user's institution, because a teacher
 * can run studies in several institutions (spec 004, D14; plan D14, T4).
 *
 * @author craulgomez
 */

import { adminService } from '@/features/admin/services/adminService'
import { CharacterizationSettings } from '@/types'
import { studyService } from './studyService'

export async function settingsOfStudy(studyId: number): Promise<CharacterizationSettings | undefined> {
  const [study, institutions] = await Promise.all([studyService.get(studyId), adminService.getInstitutions()])
  return institutions.find((item) => item.id === study.institutionId)?.settings
}
