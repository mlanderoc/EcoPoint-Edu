/**
 * Tests for authentication context, login form and session synchronization (T309 / R-310 / R-312).
 *
 * @author craulgomez
 */

import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { server } from '@/mocks/server'
import { resetMockState, setActiveUser } from '@/mocks/handlers'
import { mockUsers } from '@/mocks/data/seedData'
import { AuthProvider, LoginForm, LoginPage, useAuth } from '@/features/auth'
import { notifyUnauthorized, ApiError } from '@/services/apiClient'

function AuthStatusDisplay() {
  const { user, isAuthenticated, isLoading, logout } = useAuth()
  if (isLoading) return <div>Cargando usuario...</div>
  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'Autenticado' : 'No autenticado'}</div>
      {user && <div data-testid="user-email">{user.email}</div>}
      {user && <div data-testid="user-role">{user.role}</div>}
      <button onClick={logout}>Cerrar sesión</button>
    </div>
  )
}

describe('Authentication & Session Handling (T309, R-310, R-312)', () => {
  it('loginFormSuccessAndSessionHandling', async () => {
    // Start with no active session in the backend mock
    resetMockState(null)
    const user = userEvent.setup()
    const handleSuccess = vi.fn()

    let meCalls = 0
    server.use(
      http.get('/api/v1/auth/me', () => {
        meCalls++
        return undefined // falls through to standard mock handler
      })
    )

    render(
      <AuthProvider skipInitialFetch>
        <LoginForm onSuccess={handleSuccess} />
      </AuthProvider>
    )

    const emailInput = screen.getByLabelText(/Correo Institucional/i)
    const passwordInput = screen.getByLabelText(/Contraseña/i)
    const submitButton = screen.getByRole('button', { name: /Iniciar Sesión/i })

    // Fill valid credentials for Docente (docente.demo@unicesar.edu.co / EcoPoint2026)
    await user.type(emailInput, 'docente.demo@unicesar.edu.co')
    await user.type(passwordInput, 'EcoPoint2026')
    await user.click(submitButton)

    // Wait for success callback
    await waitFor(() => {
      expect(handleSuccess).toHaveBeenCalledTimes(1)
    })

    const authenticatedUser = handleSuccess.mock.calls[0][0]
    expect(authenticatedUser.email).toBe('docente.demo@unicesar.edu.co')
    expect(authenticatedUser.role).toBe('DOCENTE')
    expect(meCalls).toBeGreaterThanOrEqual(1)
  })

  it('validates required fields and email format before submitting', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider skipInitialFetch>
        <LoginForm />
      </AuthProvider>
    )

    const submitButton = screen.getByRole('button', { name: /Iniciar Sesión/i })
    await user.click(submitButton)

    expect(
      await screen.findByText('Ingrese su correo electrónico institucional.')
    ).toBeInTheDocument()
    expect(await screen.findByText('Ingrese su contraseña.')).toBeInTheDocument()

    // Invalid email format
    const emailInput = screen.getByLabelText(/Correo Institucional/i)
    await user.type(emailInput, 'correo-invalido')
    await user.click(submitButton)

    expect(await screen.findByText('El formato de correo no es válido.')).toBeInTheDocument()
  })

  it('displays backend error message when credentials are wrong', async () => {
    resetMockState(null)
    const user = userEvent.setup()

    render(
      <AuthProvider skipInitialFetch>
        <LoginForm />
      </AuthProvider>
    )

    const emailInput = screen.getByLabelText(/Correo Institucional/i)
    const passwordInput = screen.getByLabelText(/Contraseña/i)
    const submitButton = screen.getByRole('button', { name: /Iniciar Sesión/i })

    await user.type(emailInput, 'usuario@unicesar.edu.co')
    await user.type(passwordInput, 'ContrasenaIncorrecta123*')
    await user.click(submitButton)

    expect(
      await screen.findByText('Correo electrónico o contraseña incorrectos.')
    ).toBeInTheDocument()
  })

  it('displays session expired alert banner when notified by HTTP 401', async () => {
    resetMockState(null)
    render(
      <AuthProvider initialUser={mockUsers[0]}>
        <LoginForm />
      </AuthProvider>
    )

    // Trigger an unauthorized notification to simulate global session expiry
    act(() => {
      notifyUnauthorized(
        new ApiError({
          timestamp: new Date().toISOString(),
          status: 401,
          errorCode: 'ERR_UNAUTHORIZED',
          message: 'Su sesión expiró.',
          path: '/api/v1/shifts',
          fieldErrors: [],
        })
      )
    })

    expect(
      await screen.findByText('Su sesión expiró. Ingrese nuevamente sus credenciales.')
    ).toBeInTheDocument()
  })

  it('syncs user and supports logout', async () => {
    setActiveUser(mockUsers[0])
    const user = userEvent.setup()

    render(
      <AuthProvider>
        <AuthStatusDisplay />
      </AuthProvider>
    )

    expect(await screen.findByText('Autenticado')).toBeInTheDocument()
    expect(screen.getByTestId('user-email')).toHaveTextContent('docente.demo@unicesar.edu.co')
    expect(screen.getByTestId('user-role')).toHaveTextContent('DOCENTE')

    const logoutButton = screen.getByRole('button', { name: /Cerrar sesión/i })
    await user.click(logoutButton)

    expect(await screen.findByText('No autenticado')).toBeInTheDocument()
  })

  it('renders LoginPage with institutional branding and touch ergonomics', () => {
    render(
      <AuthProvider skipInitialFetch>
        <LoginPage />
      </AuthProvider>
    )

    expect(screen.getByText('EcoPoint Edu')).toBeInTheDocument()
    expect(
      screen.getByText('Caracterización Participativa de Residuos Sólidos')
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Iniciar Sesión/i })).toHaveClass('min-h-[48px]')
  })
})
