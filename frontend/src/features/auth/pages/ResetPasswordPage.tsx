/**
 * Creates the new password from the link received by email (spec D18, RP-06 to
 * RP-08). The code is removed from the address bar as soon as the page opens.
 *
 * @author craulgomez
 */

import React, { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { AlertBanner, Button, Input } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { AuthLayout } from '../components/AuthLayout'
import { authService } from '../services/authService'

const MIN_PASSWORD_LENGTH = 8
const INVALID_LINK = 'El enlace no es válido o ya venció. Solicita uno nuevo.'

interface FieldErrors {
  next?: string
  confirmation?: string
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const [token] = useState(() => searchParams.get('token')?.trim() ?? '')
  const [next, setNext] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [linkError, setLinkError] = useState<string | null>(token ? null : INVALID_LINK)
  const [serverError, setServerError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    window.history.replaceState(window.history.state, '', window.location.pathname)
  }, [])

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setServerError(null)
    const found: FieldErrors = {}
    if (next.length < MIN_PASSWORD_LENGTH) {
      found.next = `La contraseña debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres.`
    }
    if (next !== confirmation) found.confirmation = 'La confirmación no coincide con la contraseña nueva.'
    setErrors(found)
    if (Object.keys(found).length > 0) return
    setIsSubmitting(true)
    try {
      await authService.confirmPasswordReset(token, next)
      setDone(true)
    } catch (err) {
      if (err instanceof ApiError) {
        const passwordError = err.getFieldError('newPassword')
        if (passwordError) {
          setErrors({ next: passwordError })
        } else {
          setLinkError(err.message || INVALID_LINK)
        }
      } else {
        setServerError('No se pudo conectar con el servidor. Intente nuevamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Contraseña nueva"
      subtitle="Escribe la contraseña que usarás desde ahora"
      backTo="/login"
      backLabel="Volver a iniciar sesión"
    >
      {done && (
        <div className="flex flex-col gap-4">
          <AlertBanner
            variant="success"
            title="Contraseña cambiada"
            message="Ya puedes iniciar sesión con tu contraseña nueva. Cerramos las sesiones que tenías abiertas."
          />
          <Link className="lg-link" to="/login">Iniciar sesión</Link>
        </div>
      )}
      {!done && linkError && (
        <div className="flex flex-col gap-4">
          <AlertBanner variant="danger" title="Enlace no válido" message={linkError} />
          <Link className="lg-link" to="/recuperar-contrasena">Solicitar un enlace nuevo</Link>
        </div>
      )}
      {!done && !linkError && (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 w-full">
          {serverError && (
            <AlertBanner variant="danger" message={serverError} onDismiss={() => setServerError(null)} />
          )}
          <div className="lg-password">
            <Input
              id="reset-password"
              name="newPassword"
              type={showPassword ? 'text' : 'password'}
              label="Contraseña nueva"
              placeholder={`Mínimo ${MIN_PASSWORD_LENGTH} caracteres`}
              value={next}
              onChange={(event) => setNext(event.target.value)}
              error={errors.next}
              required
              autoComplete="new-password"
            />
            <button
              type="button"
              className="lg-eye"
              aria-label={showPassword ? 'Ocultar clave' : 'Mostrar clave'}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((shown) => !shown)}
            >
              {showPassword ? <Eye className="w-5 h-5" /> : <EyeOff className="w-5 h-5" />}
            </button>
          </div>
          <Input
            id="reset-confirmation"
            name="confirmation"
            type={showPassword ? 'text' : 'password'}
            label="Confirmar contraseña nueva"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            error={errors.confirmation}
            required
            autoComplete="new-password"
          />
          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full min-h-[48px] text-base font-bold"
            isLoading={isSubmitting}
          >
            Guardar contraseña
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}
