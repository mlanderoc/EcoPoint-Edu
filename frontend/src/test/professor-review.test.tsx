/**
 * Corrections asked by the professor: logical limits, Valledupar addresses
 * and emails with @, Rokko's guide, day and shift tooltip, choices without a
 * default value, greeting with today's date and the link to the profile.
 *
 * @author craulgomez
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '@/features/auth'
import { NewBagPage } from '@/features/field'
import { Header, Sidebar } from '@/components/layout'
import { Select } from '@/components/ui'
import { RokkoTip } from '@/components/guide/RokkoTip'
import { StudyTrail, slotName } from '@/features/home/components/StudyTrail'
import { StudyDatesEditor } from '@/features/study/components/StudyDatesEditor'
import { EMPTY_ADDRESS, addressErrors, composeAddress } from '@/features/admin/components/AddressFields'
import { pastDateErrors } from '@/features/study/utils/samplingDays'
import { emailError } from '@/utils/email'
import { chosenOrOnly } from '@/utils/choice'
import { mockUsers } from '@/mocks/data/seedData'
import { resetMockState } from '@/mocks/handlers'

const teacher = mockUsers[0]

describe('Point 1: logical limits', () => {
  it('rev1_bagOutsideTheLimitsShowsTheErrorAndCannotBeSaved', async () => {
    resetMockState('ESTUDIANTE_UNIVERSITARIO')
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/campo/turno/14/bolsas/nueva']}>
        <AuthProvider initialUser={mockUsers[1]}>
          <Routes><Route path="/campo/turno/:id/bolsas/nueva" element={<NewBagPage />} /></Routes>
        </AuthProvider>
      </MemoryRouter>
    )
    await user.click(await screen.findByRole('button', { name: /AU Aulas de Clase/i }))
    await user.type(screen.getByLabelText('Peso bruto (kg)'), '75')
    await user.type(screen.getByLabelText('Tara del recipiente (kg)'), '0,40')
    await user.type(screen.getByLabelText('Volumen de la bolsa (m³)'), '0,080')

    expect(screen.getByText('El peso bruto debe estar entre 0,01 y 60 kg.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar bolsa' })).toBeDisabled()
  })
})

describe('Point 2: addresses of Valledupar and emails', () => {
  it('rev2_guidedAddressIsComposedWithTheCity', () => {
    const address = { ...EMPTY_ADDRESS, road: 'Calle', number: '16', letter: 'A', cross: '12', crossLetter: 'B', plate: '45',
      neighbourhood: 'Novalito' }
    expect(addressErrors(address)).toEqual({})
    expect(composeAddress(address)).toBe('Calle 16A # 12B - 45, Novalito, Valledupar, Cesar')
    expect(composeAddress({ ...EMPTY_ADDRESS, road: 'Kilómetro', number: '5', destination: 'La Paz' }))
      .toBe('Kilómetro 5 vía a La Paz, Valledupar, Cesar')
  })

  it('rev2_illogicalAddressNumbersAreRejected', () => {
    const errors = addressErrors({ ...EMPTY_ADDRESS, road: 'Carrera', number: '151', cross: '0', plate: '200' })
    expect(errors.number).toBe('El número de la vía debe estar entre 1 y 150.')
    expect(errors.cross).toBe('El número después del # debe estar entre 1 y 150.')
    expect(errors.plate).toBe('El número de la placa debe estar entre 1 y 199.')
    expect(addressErrors(EMPTY_ADDRESS).road).toBe('Escoja el tipo de vía.')
  })

  it('rev2_emailWithoutAtIsRejected', () => {
    expect(emailError('docente.unicesar.edu.co')).toBe('Falta el @ en el correo.')
    expect(emailError('docente@unicesar')).toMatch(/dominio/)
    expect(emailError('docente@unicesar.edu.co')).toBeUndefined()
  })
})

describe('Point 3: Rokko guides each screen', () => {
  beforeEach(() => window.localStorage.clear())

  it('rev3_rokkoShowsTheStepsAndCanBeFoldedAway', async () => {
    const user = userEvent.setup()
    const { unmount } = render(<RokkoTip tip="sources" />)

    expect(screen.getByText('Registrar una fuente de residuos')).toBeInTheDocument()
    expect(screen.getByText('Escribe el nombre del espacio y escoge su tipo. El código se crea solo.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ocultar la guía de Rokko' }))
    expect(screen.queryByText('Registrar una fuente de residuos')).toBeNull()
    unmount()

    render(<RokkoTip tip="sources" />)
    await user.click(screen.getByRole('button', { name: /Ver la guía de Rokko/ }))
    expect(screen.getByText('Registrar una fuente de residuos')).toBeInTheDocument()
  })
})

describe('Points 5 and 6: study dates and shift tooltip', () => {
  it('rev5_datesBeforeTodayShowAnError', () => {
    const [past, today] = pastDateErrors(['2026-11-24', '2026-11-25'], [], '2026-11-25')
    expect(past).toMatch(/^La fecha no puede ser anterior a hoy \(.*25.*nov.*2026\)\.$/)
    expect(today).toBeUndefined()
    expect(pastDateErrors(['2026-11-20'], ['2026-11-20'], '2026-11-25')).toEqual([undefined])

    render(<StudyDatesEditor dates={['2026-11-20', '', '', '', '', '', '', '']} onChange={() => undefined}
      now={new Date(2026, 10, 25, 9)} />)
    expect(screen.getByLabelText('Día 1 (descarte)')).toHaveAttribute('min', '2026-11-25')
    expect(screen.getByText(/La fecha no puede ser anterior a hoy/)).toBeInTheDocument()
  })

  it('rev6_rokkoSaysTheDayAndShiftOfTheStopUnderThePointer', async () => {
    const user = userEvent.setup()
    const said: (string | null)[] = []
    const slots = [
      { dayNumber: 3, type: 'MANANA', shiftId: null, status: null, coveredSources: 0, totalSources: 6 },
      { dayNumber: 3, type: 'TARDE', shiftId: null, status: null, coveredSources: 0, totalSources: 6 },
    ] as never[]
    render(<StudyTrail selected={0} onSelect={() => undefined} slots={slots}
      onHover={(index) => said.push(index === null ? null : slotName(slots[index]))} />)

    await user.hover(screen.getByRole('button', { name: 'Día 3, Tarde' }))
    await user.unhover(screen.getByRole('button', { name: 'Día 3, Tarde' }))
    expect(said).toEqual(['Día 3, Turno Tarde', null])
  })
})

describe('Point 7: choices without a default value', () => {
  it('rev7_severalOptionsWaitForTheUserAndASingleOneIsChosenAlone', () => {
    const one = [{ id: 1 }]
    const two = [{ id: 1 }, { id: 2 }]
    expect(chosenOrOnly(one, null)?.id).toBe(1)
    expect(chosenOrOnly(two, null)).toBeNull()
    expect(chosenOrOnly(two, 2)?.id).toBe(2)

    render(<Select id="choice" label="Estudio" value="" placeholder="Escoja un estudio" onChange={() => undefined}
      options={[{ value: '1', label: 'Estudio 1' }, { value: '2', label: 'Estudio 2' }]} />)
    expect(screen.getByLabelText('Estudio')).toHaveValue('')
    expect(screen.getByRole('option', { name: 'Escoja un estudio' })).toBeDisabled()
  })
})

describe('Points 8 and 9: greeting and profile', () => {
  it('rev8_headerGreetsWithTheNameAndTodaysDate', () => {
    render(<Header user={teacher} currentPath="/campo" now={new Date(2026, 10, 25, 8, 30)} />)

    const greeting = screen.getByTestId('greeting')
    expect(greeting).toHaveTextContent(`Buenos días, ${teacher.firstName}`)
    expect(greeting).toHaveTextContent('miércoles, 25 de noviembre de 2026')
  })

  it('rev8_afternoonAndNightGreetings', () => {
    const { rerender } = render(<Header user={teacher} currentPath="/campo" now={new Date(2026, 10, 25, 15)} />)
    expect(screen.getByTestId('greeting')).toHaveTextContent('Buenas tardes')
    rerender(<Header user={teacher} currentPath="/campo" now={new Date(2026, 10, 25, 21)} />)
    expect(screen.getByTestId('greeting')).toHaveTextContent('Buenas noches')
  })

  it('rev9_userCardOpensTheProfile', async () => {
    const user = userEvent.setup()
    let target = ''
    render(
      <MemoryRouter>
        <AuthProvider initialUser={teacher}>
          <Sidebar role="DOCENTE" user={teacher} currentPath="/inicio" onNavigate={(href) => { target = href }} />
        </AuthProvider>
      </MemoryRouter>
    )

    await user.click(screen.getByTitle('Ver mi perfil'))
    expect(target).toBe('/perfil')
  })
})
