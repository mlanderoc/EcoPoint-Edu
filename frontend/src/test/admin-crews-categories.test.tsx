/**
 * Integration tests for Crews and Waste Categories Administration (T313 / R-323 / R-324 / RN-085).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { server } from '@/mocks/server'
import { AuthProvider } from '@/features/auth'
import { CrewsPage } from '@/features/admin/pages/CrewsPage'
import { CategoriesPage } from '@/features/admin/pages/CategoriesPage'
import { mockUsers, mockCrews } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

describe('Crews and Categories Administration (T313, R-323, R-324, RN-085)', () => {
  const teacherUser = mockUsers[0]

  it('renders crews list with responsible teacher and displays warning if missing (RN-085)', async () => {
    resetMockState('DOCENTE')

    // Inject a crew without responsible teacher to test the visual RN-085 warning
    server.use(
      http.get('/api/v1/crews', () => {
        return HttpResponse.json([
          ...mockCrews,
          {
            id: 99,
            name: 'Cuadrilla Sin Docente',
            responsibleId: 0,
            memberIds: [2],
          },
        ])
      })
    )

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <CrewsPage />
        </AuthProvider>
      </MemoryRouter>
    )

    // Verify initial crews loaded
    expect(await screen.findByText('Cuadrilla Mañana A')).toBeInTheDocument()
    expect(screen.getAllByText(/Docente Responsable/i).length).toBeGreaterThan(0)

    // RN-085 visual warning test for the crew without teacher
    expect(await screen.findByText('Cuadrilla Sin Docente')).toBeInTheDocument()
    const warning = await screen.findByText(/Cuadrilla sin docente responsable/i)
    expect(warning).toBeInTheDocument()
    expect(screen.getByText(/no cuenta con un docente responsable asignado/i)).toBeInTheDocument()
  })

  it('validates teacher is required when creating a crew and saves successfully', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <CrewsPage />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Cuadrilla Mañana A')).toBeInTheDocument()

    const newBtn = screen.getByRole('button', { name: /Nueva Cuadrilla/i })
    expect(newBtn).toHaveClass('min-h-[48px]')
    await user.click(newBtn)

    expect(await screen.findByText('Crear Cuadrilla de Muestreo')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Nombre de la Cuadrilla/i)
    const teacherSelect = screen.getByLabelText(/Docente Responsable/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Cuadrilla/i })

    await user.type(nameInput, 'Cuadrilla Sábado C')

    // If teacher is cleared, warning is shown and button is disabled
    await user.selectOptions(teacherSelect, '')
    expect(await screen.findByText(/Es obligatorio asignar un docente responsable/i)).toBeInTheDocument()
    expect(saveBtn).toBeDisabled()

    // Select teacher and submit
    await user.selectOptions(teacherSelect, String(teacherUser.id))
    expect(saveBtn).not.toBeDisabled()
    await user.click(saveBtn)

    // Verify new crew appears
    expect(await screen.findByText('Cuadrilla Sábado C')).toBeInTheDocument()
  })

  it('renders categories grouped by color and allows creating a new subcategory (R-324)', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <CategoriesPage />
        </AuthProvider>
      </MemoryRouter>
    )

    // Verify the 4 color groups are rendered
    expect(await screen.findByText('Blanco')).toBeInTheDocument()
    expect(screen.getByText('Verde Orgánico')).toBeInTheDocument()
    expect(screen.getByText('Negro')).toBeInTheDocument()
    expect(screen.getByText('Sin color')).toBeInTheDocument()

    // Verify initial categories exist
    expect(screen.getByText('Papel de archivo')).toBeInTheDocument()
    expect(screen.getByText('Restos de comida')).toBeInTheDocument()
    expect(screen.getByText('Pilas y baterías')).toBeInTheDocument()

    // Open modal to add category
    const newBtn = screen.getByRole('button', { name: /Nueva Subcategoría/i })
    expect(newBtn).toHaveClass('min-h-[48px]')
    await user.click(newBtn)

    expect(await screen.findByText('Registrar Subcategoría de Residuo')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Nombre de la Subcategoría/i)
    const groupSelect = screen.getByLabelText(/Grupo de Color/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Subcategoría/i })

    await user.type(nameInput, 'Vidrio transparente')
    await user.selectOptions(groupSelect, 'BLANCO')
    await user.click(saveBtn)

    // Verify new subcategory is visible
    expect(await screen.findByText('Vidrio transparente')).toBeInTheDocument()
  })
})
