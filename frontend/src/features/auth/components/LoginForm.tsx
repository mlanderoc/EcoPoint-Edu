/**
 * Login form component with real-time validation and session expiry alert (T309 / R-310 / R-312).
 *
 * @author craulgomez
 */

import React, { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { z } from 'zod'
import { Button, Input, AlertBanner } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { User } from '@/types'
import { useAuth } from '../hooks/useAuth'

const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Ingrese su correo electrónico institucional.')
    .email('El formato de correo no es válido.'),
  password: z.string().min(1, 'Ingrese su contraseña.'),
})

export interface LoginFormProps {
  onSuccess?: (user: User) => void
  className?: string
}

export function LoginForm({ onSuccess, className = '' }: LoginFormProps) {
  const { login, sessionExpired, clearSessionExpired } = useAuth()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setServerError(null)

    // Client-side schema validation
    const result = loginSchema.safeParse({ email: email.trim(), password })
    if (!result.success) {
      const fieldErrors: { email?: string; password?: string } = {}
      for (const issue of result.error.issues) {
        const fieldName = issue.path[0] as 'email' | 'password'
        if (!fieldErrors[fieldName]) {
          fieldErrors[fieldName] = issue.message
        }
      }
      setErrors(fieldErrors)
      return
    }

    setErrors({})
    setIsSubmitting(true)

    try {
      const user = await login({ email: email.trim(), password })
      onSuccess?.(user)
    } catch (err) {
      if (err instanceof ApiError) {
        setServerError(err.message || 'Error al iniciar sesión. Verifique sus credenciales.')
        const emailErr = err.getFieldError('email')
        const passErr = err.getFieldError('password')
        if (emailErr || passErr) {
          setErrors({
            email: emailErr,
            password: passErr,
          })
        }
      } else {
        setServerError('No se pudo conectar con el servidor. Intente nuevamente.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className={`flex flex-col gap-4 w-full ${className}`}>
      {/* Session Expired Banner (R-312) */}
      {sessionExpired && (
        <AlertBanner
          variant="warning"
          title="Sesión expirada"
          message="Su sesión expiró. Ingrese nuevamente sus credenciales."
          onDismiss={clearSessionExpired}
        />
      )}

      {/* Server Error Alert */}
      {serverError && (
        <AlertBanner
          variant="danger"
          title="Error de autenticación"
          message={serverError}
          onDismiss={() => setServerError(null)}
        />
      )}

      <div className="flex flex-col gap-4">
        <Input
          id="login-email"
          name="email"
          type="email"
          inputMode="email"
          label="Correo institucional"
          placeholder="nombre@unicesar.edu.co"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            if (errors.email) setErrors((prev) => ({ ...prev, email: undefined }))
          }}
          error={errors.email}
          required
          autoComplete="email"
        />

        <div className="lg-password">
          <Input
            id="login-password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            label="Contraseña"
            placeholder="Tu contraseña"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (errors.password) setErrors((prev) => ({ ...prev, password: undefined }))
            }}
            error={errors.password}
            required
            autoComplete="current-password"
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
      </div>

      <Button
        type="submit"
        variant="primary"
        size="lg"
        className="w-full min-h-[48px] text-base font-bold"
        isLoading={isSubmitting}
      >
        Iniciar sesión
      </Button>
    </form>
  )
}
