/**
 * Integration tests for institutions and generation sources administration (T311 / R-320 / R-321).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { InstitutionsPage } from '@/features/admin/pages/InstitutionsPage'
import { SourcesPage } from '@/features/admin/pages/SourcesPage'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

describe('Institutions and Generation Sources Administration (T311, R-320, R-321)', () => {
  const teacherUser = mockUsers[0]

  it('renders institutions list and allows creating a new institution', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <InstitutionsPage />
        </AuthProvider>
      </MemoryRouter>
    )

    // Verify initial institution is listed
    expect(await screen.findByText('I.E. Prudencia Daza')).toBeInTheDocument()
    expect(screen.getByText(/Calle 16 # 19-32/i)).toBeInTheDocument()

    // Click "Nueva Institución"
    const newBtn = screen.getByRole('button', { name: /Nueva Institución/i })
    expect(newBtn).toHaveClass('min-h-[48px]')
    await user.click(newBtn)

    // Modal opens
    expect(await screen.findByText('Registrar Institución Educativa')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Nombre de la Institución/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Institución/i })

    await user.type(nameInput, 'Colegio Nacional Loperena')
    await user.selectOptions(screen.getByLabelText('Tipo de vía'), 'Carrera')
    await user.type(screen.getByLabelText('Número de la carrera'), '9')
    await user.type(screen.getByLabelText('#'), '15')
    await user.type(screen.getByLabelText('-'), '20')
    expect(screen.getByTestId('address-preview')).toHaveTextContent('Carrera 9 # 15 - 20, Valledupar, Cesar')
    await user.click(saveBtn)

    // Verify created institution appears in the list
    expect(await screen.findByText('Colegio Nacional Loperena')).toBeInTheDocument()
    expect(screen.getByText('Carrera 9 # 15 - 20, Valledupar, Cesar')).toBeInTheDocument()
  })

  it('renders generation sources list and allows adding a new source linked to an institution', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <SourcesPage />
        </AuthProvider>
      </MemoryRouter>
    )

    // Verify initial generation sources are listed
    expect(await screen.findByText('Aulas de Clase')).toBeInTheDocument()
    expect(screen.getByText('AU')).toBeInTheDocument()
    expect(screen.getByText('Baterías Sanitarias')).toBeInTheDocument()

    // Click "Nueva Fuente"
    const newBtn = screen.getByRole('button', { name: /Nueva Fuente/i })
    expect(newBtn).toHaveClass('min-h-[48px]')
    await user.click(newBtn)

    // Modal opens
    expect(await screen.findByText('Crear Fuente de Generación')).toBeInTheDocument()

    expect(screen.queryByLabelText(/Código de Fuente/i)).toBeNull()
    const nameInput = screen.getByLabelText(/Nombre Descriptivo/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Fuente/i })

    await user.type(nameInput, 'Laboratorio de Química')
    await user.selectOptions(screen.getByLabelText('Tipo de Espacio'), 'LABORATORIO')
    await user.click(saveBtn)

    // The code is generated from the name (professor's review, point 4)
    expect(await screen.findByText('Laboratorio de Química')).toBeInTheDocument()
    expect(screen.getByText('LQ')).toBeInTheDocument()
  })
})
