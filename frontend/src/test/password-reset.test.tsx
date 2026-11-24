/**
 * Tests for the password recovery screens (spec D18, CP-01, CP-03, CP-04 and CP-08).
 *
 * @author craulgomez
 */

import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { server } from '@/mocks/server'
import { MOCK_RESET_TOKEN } from '@/mocks/handlers'
import { AuthProvider, ForgotPasswordPage, LoginPage, ResetPasswordPage } from '@/features/auth'

function renderAt(path: string) {
  return render(
    <AuthProvider skipInitialFetch>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/recuperar-contrasena" element={<ForgotPasswordPage />} />
          <Route path="/restablecer-contrasena" element={<ResetPasswordPage />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  )
}

describe('Password recovery (spec D18)', () => {
  it('cp01_loginLeadsToTheRecoveryScreen', async () => {
    const user = userEvent.setup()
    renderAt('/login')

    await user.click(screen.getByRole('link', { name: '¿Olvidaste tu contraseña?' }))

    expect(screen.getByRole('heading', { name: 'Recuperar contraseña' })).toBeInTheDocument()
  })

  it('cp01_requestShowsTheSameAnswerForAnyEmail', async () => {
    const user = userEvent.setup()
    let sentEmail: string | undefined
    server.use(
      http.post('/api/v1/auth/password-reset/request', async ({ request }) => {
        sentEmail = ((await request.json()) as { email: string }).email
        return new HttpResponse(null, { status: 204 })
      })
    )
    renderAt('/recuperar-contrasena')

    await user.type(screen.getByLabelText(/Correo institucional/i), '  nadie@unicesar.edu.co ')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByText(/Si el correo está registrado, te llegará un enlace/)).toBeInTheDocument()
    expect(sentEmail).toBe('nadie@unicesar.edu.co')
  })

  it('cp01_invalidEmailIsNotSent', async () => {
    const user = userEvent.setup()
    renderAt('/recuperar-contrasena')

    await user.type(screen.getByLabelText(/Correo institucional/i), 'correo-sin-arroba')
    await user.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(screen.getByText('El formato de correo no es válido.')).toBeInTheDocument()
  })

  it('cp03_validLinkSavesTheNewPassword', async () => {
    const user = userEvent.setup()
    renderAt(`/restablecer-contrasena?token=${MOCK_RESET_TOKEN}`)

    await user.type(screen.getByLabelText('Contraseña nueva'), 'ClaveNueva2026')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'ClaveNueva2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('Contraseña cambiada')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('cp04_invalidLinkOffersANewOne', async () => {
    const user = userEvent.setup()
    renderAt('/restablecer-contrasena?token=enlace-vencido')

    await user.type(screen.getByLabelText('Contraseña nueva'), 'ClaveNueva2026')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'ClaveNueva2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText(/El enlace no es válido o ya venció/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solicitar un enlace nuevo' })).toBeInTheDocument()
  })

  it('cp04_linkWithoutCodeIsInvalidFromTheStart', () => {
    renderAt('/restablecer-contrasena')

    expect(screen.getByText(/El enlace no es válido o ya venció/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar contraseña' })).not.toBeInTheDocument()
  })

  it('cp08_shortOrDifferentPasswordIsRejectedBeforeSending', async () => {
    const user = userEvent.setup()
    renderAt(`/restablecer-contrasena?token=${MOCK_RESET_TOKEN}`)

    await user.type(screen.getByLabelText('Contraseña nueva'), 'corta')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'otra')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(screen.getByText('La contraseña debe tener al menos 8 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('La confirmación no coincide con la contraseña nueva.')).toBeInTheDocument()
  })

  it('cp08_passwordRejectedByTheApiShowsNextToTheField', async () => {
    const user = userEvent.setup()
    server.use(
      http.post('/api/v1/auth/password-reset/confirm', () =>
        HttpResponse.json(
          {
            status: 400,
            errorCode: 'ERR_INVALID_DATA',
            message: 'La contraseña es demasiado larga.',
            path: '/api/v1/auth/password-reset/confirm',
            fieldErrors: [{ field: 'newPassword', message: 'La contraseña es demasiado larga.' }],
          },
          { status: 400 }
        )
      )
    )
    renderAt(`/restablecer-contrasena?token=${MOCK_RESET_TOKEN}`)

    await user.type(screen.getByLabelText('Contraseña nueva'), 'ClaveNueva2026')
    await user.type(screen.getByLabelText('Confirmar contraseña nueva'), 'ClaveNueva2026')
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }))

    expect(await screen.findByText('La contraseña es demasiado larga.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar contraseña' })).toBeInTheDocument()
  })
})
