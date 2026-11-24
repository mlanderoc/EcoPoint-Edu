/**
 * Asks for a link to create a new password (spec D18, RP-01). The answer is the
 * same whether the email has an account or not.
 *
 * @author craulgomez
 */

import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { AlertBanner, Button, Input } from '@/components/ui'
import { AuthLayout } from '../components/AuthLayout'
import { authService } from '../services/authService'

const emailSchema = z
  .string()
  .min(1, 'Ingrese su correo electrónico institucional.')
  .email('El formato de correo no es válido.')

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string>()
  const [serverError, setServerError] = useState<string | null>(null)
  const [sent, setSent] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setServerError(null)
    const result = emailSchema.safeParse(email.trim())
    if (!result.success) {
      setError(result.error.issues[0].message)
      return
    }
    setError(undefined)
    setIsSubmitting(true)
    try {
      await authService.requestPasswordReset(email.trim())
      setSent(true)
    } catch {
      setServerError('No se pudo conectar con el servidor. Intente nuevamente.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Recuperar contraseña"
      subtitle="Te enviaremos un enlace a tu correo"
      backTo="/login"
      backLabel="Volver a iniciar sesión"
    >
      {sent ? (
        <div className="flex flex-col gap-4">
          <AlertBanner
            variant="success"
            title="Revisa tu correo"
            message="Si el correo está registrado, te llegará un enlace en unos minutos. Vence en 30 minutos. Si no lo ves, revisa la carpeta de spam."
          />
          <Link className="lg-link" to="/login">Volver a iniciar sesión</Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 w-full">
          {serverError && (
            <AlertBanner variant="danger" message={serverError} onDismiss={() => setServerError(null)} />
          )}
          <Input
            id="forgot-email"
            name="email"
            type="email"
            inputMode="email"
            label="Correo institucional"
            placeholder="nombre@unicesar.edu.co"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value)
              if (error) setError(undefined)
            }}
            error={error}
            required
            autoComplete="email"
          />
          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full min-h-[48px] text-base font-bold"
            isLoading={isSubmitting}
          >
            Enviar enlace
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}
