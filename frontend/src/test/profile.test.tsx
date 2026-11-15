/**
 * "Mi Perfil": own data, own totals and change of password (spec 004, R-350 to
 * R-352, CA-314).
 *
 * @author craulgomez
 */
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { fieldService } from '@/features/field'
import { ProfilePage, profileService } from '@/features/profile'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

/** Shift 14 of the mock: day 3 morning, open, with population. */
const OPEN_SHIFT = 14
const student = mockUsers[1]

function renderProfile() {
  return render(
    <MemoryRouter>
      <AuthProvider initialUser={student}>
        <ProfilePage />
      </AuthProvider>
    </MemoryRouter>
  )
}

describe('Mi Perfil (R-350 a R-352, CA-314)', () => {
  it('ca314_studentSeesHisDataAndHisOwnBags', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const before = (await profileService.get()).activity.bags
    const shift = await fieldService.getShift(OPEN_SHIFT)
    for (let bag = 0; bag < 3; bag++) {
      await fieldService.registerBag(OPEN_SHIFT, {
        sourceId: shift.sources[0].sourceId, grossWeight: 5, tare: 0.1, volume: 0.05,
      })
    }

    renderProfile()

    expect(await screen.findByText(student.email)).toBeInTheDocument()
    expect(screen.getByText('Estudiante universitario')).toBeInTheDocument()
    expect(screen.getByLabelText(`Bolsas: ${before + 3}`)).toBeInTheDocument()
    expect(screen.queryByText(mockUsers[0].email)).toBeNull()
  })

  it('r352_wrongCurrentPasswordIsShownOnItsField', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    renderProfile()

    await user.type(await screen.findByLabelText('Contraseña actual'), 'Equivocada1')
    await user.type(screen.getByLabelText('Contraseña nueva'), 'NuevaClave2026')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'NuevaClave2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('La contraseña actual no es correcta.')).toBeInTheDocument()
  })

  it('r352_passwordChangesWithTheRightCurrentOne', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    renderProfile()

    await user.type(await screen.findByLabelText('Contraseña actual'), 'EcoPoint2026')
    await user.type(screen.getByLabelText('Contraseña nueva'), 'NuevaClave2026')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'NuevaClave2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña actual')).toHaveValue('')
  })
})
