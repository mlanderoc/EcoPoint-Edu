/**
 * Login page view with the campus photo and Eco greeting (T309 / R-310; D15),
 * and the link to recover the password (spec D18).
 *
 * @author craulgomez
 */

import { Link } from 'react-router-dom'
import { User } from '@/types'
import { AuthLayout } from '../components/AuthLayout'
import { LoginForm } from '../components/LoginForm'

export interface LoginPageProps {
  onSuccess?: (user: User) => void
}

export function LoginPage({ onSuccess }: LoginPageProps) {
  return (
    <AuthLayout title="Iniciar sesión" subtitle="¡Te damos la bienvenida!" backTo="/" backLabel="Volver al inicio">
      <LoginForm onSuccess={onSuccess} />
      <Link className="lg-link" to="/recuperar-contrasena">¿Olvidaste tu contraseña?</Link>
    </AuthLayout>
  )
}
