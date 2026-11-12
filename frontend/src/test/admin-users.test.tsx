/**
 * Integration and acceptance tests for Users Administration (T312 / R-322 / CA-302).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { UsersPage } from '@/features/admin/pages/UsersPage'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

describe('Users Administration (T312, R-322, CA-302)', () => {
  const teacherUser = mockUsers[0]

  it('ca302_duplicateEmailDisplaysFieldError', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <UsersPage />
        </AuthProvider>
      </MemoryRouter>
    )

    // Verify initial users are loaded
    expect(await screen.findByText('Docente')).toBeInTheDocument()
    expect(screen.getByText('docente.demo@unicesar.edu.co')).toBeInTheDocument()

    // Open "Nuevo Usuario" modal
    const newBtn = screen.getByRole('button', { name: /Nuevo Usuario/i })
    expect(newBtn).toHaveClass('min-h-[48px]')
    await user.click(newBtn)

    expect(await screen.findByText('Registrar Nuevo Usuario')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Nombres/i)
    const lastNameInput = screen.getByLabelText(/Apellidos/i)
    const emailInput = screen.getByLabelText(/Correo Institucional/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Usuario/i })

    // Fill in existing email to trigger duplicate error
    await user.type(nameInput, 'Docente')
    await user.type(lastNameInput, 'Duplicado')
    await user.type(emailInput, 'docente.demo@unicesar.edu.co')
    await user.click(saveBtn)

    // CA-302: Verify error message appears in red directly under the email field
    const errorMessage = await screen.findByRole('alert')
    expect(errorMessage).toBeInTheDocument()
    expect(errorMessage).toHaveTextContent(/El correo electrónico ya se encuentra registrado/i)
    expect(errorMessage).toHaveClass('text-red-600')
    expect(emailInput).toHaveClass('border-red-500')
  }, 15000)

  it('allows registering a new user successfully', async () => {
    resetMockState('DOCENTE')
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacherUser}>
          <UsersPage />
        </AuthProvider>
      </MemoryRouter>
    )

    expect(await screen.findByText('Docente')).toBeInTheDocument()

    const newBtn = screen.getByRole('button', { name: /Nuevo Usuario/i })
    await user.click(newBtn)

    expect(await screen.findByText('Registrar Nuevo Usuario')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Nombres/i)
    const lastNameInput = screen.getByLabelText(/Apellidos/i)
    const emailInput = screen.getByLabelText(/Correo Institucional/i)
    const roleSelect = screen.getByLabelText(/Rol en el Sistema/i)
    const saveBtn = screen.getByRole('button', { name: /Guardar Usuario/i })

    await user.type(nameInput, 'Laura')
    await user.type(lastNameInput, 'Pérez')
    await user.type(emailInput, 'laura.perez@unicesar.edu.co')
    await user.selectOptions(roleSelect, 'ESTUDIANTE_UNIVERSITARIO')
    await user.click(saveBtn)

    // Verify user is created and visible in list
    expect(await screen.findByText('Laura Pérez')).toBeInTheDocument()
    expect(screen.getByText('laura.perez@unicesar.edu.co')).toBeInTheDocument()
    expect(screen.getAllByText('Estudiante').length).toBeGreaterThan(0)
  }, 15000)
})
