/**
 * No screen shows rule codes such as "R-320" or "RN-085" (spec 004, R-306,
 * CA-317; plan D13, decision T13). The codes stay in comments and test names.
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import { AuthProvider } from '@/features/auth'
import { CategoriesPage, CrewsPage, InstitutionsPage, SourcesPage, UsersPage } from '@/features/admin'
import { FieldDashboard, NewBagPage, PopulationPage, ShiftPage } from '@/features/field'
import { AlertsPage, DensityPage, QuarteringPage, VerificationPage } from '@/features/quality'
import { NewStudyPage, StudiesPage } from '@/features/study'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

const RULE_CODE = /\b(RN|R|CA)-\d{3}\b/
/** Shift 14 of the mock: day 3 morning, open, with population and bags. */
const OPEN_SHIFT = 14

const SCREENS: Array<[string, string, string, ReactElement]> = [
  ['Instituciones', '/admin/instituciones', '/admin/instituciones', <InstitutionsPage />],
  ['Fuentes', '/admin/fuentes', '/admin/fuentes', <SourcesPage />],
  ['Usuarios', '/admin/usuarios', '/admin/usuarios', <UsersPage />],
  ['Cuadrillas', '/admin/cuadrillas', '/admin/cuadrillas', <CrewsPage />],
  ['Subcategorías', '/admin/subcategorias', '/admin/subcategorias', <CategoriesPage />],
  ['Estudios', '/estudios', '/estudios', <StudiesPage />],
  ['Nuevo estudio', '/estudios/nuevo', '/estudios/nuevo', <NewStudyPage />],
  ['Registro en campo', '/campo', '/campo', <FieldDashboard />],
  ['Turno', '/campo/turno/:id', `/campo/turno/${OPEN_SHIFT}`, <ShiftPage />],
  ['Población', '/campo/turno/:id/poblacion', `/campo/turno/${OPEN_SHIFT}/poblacion`, <PopulationPage />],
  ['Bolsa', '/campo/turno/:id/bolsas/nueva', `/campo/turno/${OPEN_SHIFT}/bolsas/nueva`, <NewBagPage />],
  ['Cuarteo', '/campo/turno/:id/cuarteo', `/campo/turno/${OPEN_SHIFT}/cuarteo`, <QuarteringPage />],
  ['Densidad', '/campo/turno/:id/densidad', `/campo/turno/${OPEN_SHIFT}/densidad`, <DensityPage />],
  ['Verificación', '/calidad/verificacion', '/calidad/verificacion', <VerificationPage />],
  ['Alertas', '/alertas', '/alertas', <AlertsPage />],
]

describe('Sin códigos de reglas en pantalla (R-306, CA-317)', () => {
  it.each(SCREENS)('ca317_%sDoesNotShowRuleCodes', async (_name, route, path, page) => {
    resetMockState('DOCENTE')
    const { container } = render(
      <MemoryRouter initialEntries={[path]}>
        <AuthProvider initialUser={mockUsers[0]}>
          <Routes>
            <Route path={route} element={page} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    )

    await screen.findAllByRole('heading')
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(container.textContent ?? '').not.toMatch(RULE_CODE)
  })
})
