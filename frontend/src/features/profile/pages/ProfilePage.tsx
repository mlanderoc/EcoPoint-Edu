/**
 * "Mi Perfil": the data of the user, his crews, his own totals of records and
 * the change of his password (spec 004, R-350 to R-352, CA-314). A student only
 * sees his own totals; the teacher sees everyone's in the users table.
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { AlertBanner, Button, Input } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { ApiError } from '@/services/apiClient'
import { UserProfile } from '@/types'
import { profileService } from '../services/profileService'

const MIN_PASSWORD_LENGTH = 8

const ROLE_NAMES: Record<string, string> = {
  DOCENTE: 'Docente',
  ESTUDIANTE_UNIVERSITARIO: 'Estudiante universitario',
}

function Datum({ label, value }: { label: string; value: string }) {
  return (
    <div><dt className="contents"><span>{label}</span></dt><dd className="contents"><b>{value}</b></dd></div>
  )
}

function Total({ label, value }: { label: string; value: number }) {
  return (
    <div role="group" aria-label={`${label}: ${value}`}>
      <b>{value}</b>
      <span>{label}</span>
    </div>
  )
}

export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [saved, setSaved] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const { confirmLeave, discardDialog } = useUnsavedChanges(!isSaving && (current + next + confirmation) !== '')

  useEffect(() => {
    profileService.get()
      .then(setProfile)
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : 'No se pudo cargar su perfil.'))
  }, [])

  const clearForm = () => {
    setCurrent('')
    setNext('')
    setConfirmation('')
    setFieldErrors({})
  }

  const handleChangePassword = async (event: React.FormEvent) => {
    event.preventDefault()
    setSaved(false)
    const errors: Record<string, string> = {}
    if (!current) errors.current = 'Escriba su contraseña actual.'
    if (next.length < MIN_PASSWORD_LENGTH) {
      errors.next = `La contraseña nueva debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres.`
    }
    if (next !== confirmation) errors.confirmation = 'La confirmación no coincide con la contraseña nueva.'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return
    try {
      setIsSaving(true)
      await profileService.changePassword(current, next)
      clearForm()
      setSaved(true)
    } catch (err) {
      if (err instanceof ApiError) {
        const currentError = err.getFieldError('currentPassword')
        const nextError = err.getFieldError('newPassword')
        setFieldErrors({
          current: currentError ?? '',
          next: nextError ?? '',
          general: currentError || nextError ? '' : err.message,
        })
      } else {
        setFieldErrors({ general: 'No se pudo cambiar la contraseña.' })
      }
    } finally {
      setIsSaving(false)
    }
  }

  if (loadError) {
    return <AlertBanner variant="danger" title="Perfil no disponible" message={loadError} />
  }
  if (!profile) {
    return <p className="text-sm text-[var(--color-text-muted)]">Cargando perfil...</p>
  }

  const { user, crews, activity } = profile

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Mi Perfil</h1>
          <p className="m-0 mt-1">Tus datos y lo que has registrado.</p>
        </div>
      </div>

      <div className="grid-left">
        <section className="panel">
          <div className="profile-hero">
            <span className="avatar">{`${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()}</span>
            <h3>{user.firstName} {user.lastName}</h3>
            <span className="pill">{ROLE_NAMES[user.role] ?? user.role}</span>
          </div>
          <dl className="data-list m-0">
            <Datum label="Correo" value={user.email} />
            {user.role === 'DOCENTE' && (
              <Datum label="Institución" value={user.institutionName ?? 'Sin institución'} />
            )}
            <Datum label="Cuadrillas" value={crews.length > 0 ? crews.join(', ') : 'Sin cuadrilla asignada'} />
          </dl>
        </section>

        <div className="flex flex-col gap-4 min-w-0">
          <section className="panel">
            <h3>Mis registros</h3>
            <div className="stat-tiles" data-testid="my-activity">
              <Total label="Poblaciones" value={activity.populations} />
              <Total label="Bolsas" value={activity.bags} />
              <Total label="Cuarteos" value={activity.quarterings} />
              <Total label="Densidades" value={activity.densities} />
              <Total label="Verificaciones hechas" value={activity.verifications} />
              <Total label="Correcciones recibidas" value={activity.correctionsReceived} />
            </div>
            <p className="m-0 text-xs text-[var(--color-text-muted)]">
              Las verificaciones son registros de otras personas que usted revisó. Las correcciones recibidas son
              cambios que otra persona hizo a datos que usted digitó.
            </p>
          </section>

          <section className="panel">
            <h3>Cambiar contraseña</h3>
            <form onSubmit={handleChangePassword} className="flex flex-col gap-4">
              {saved && <AlertBanner variant="success" message="Contraseña cambiada." onDismiss={() => setSaved(false)} />}
              {fieldErrors.general && <AlertBanner variant="danger" message={fieldErrors.general} />}
              <div className="form-row">
                <Input id="password-current" label="Contraseña actual" type="password" autoComplete="current-password"
                  value={current} onChange={(event) => setCurrent(event.target.value)}
                  error={fieldErrors.current || undefined} disabled={isSaving} />
                <Input id="password-new" label="Contraseña nueva" type="password" autoComplete="new-password"
                  value={next} onChange={(event) => setNext(event.target.value)}
                  helperText={`Mínimo ${MIN_PASSWORD_LENGTH} caracteres`} error={fieldErrors.next || undefined}
                  disabled={isSaving} />
                <Input id="password-confirmation" label="Confirmar contraseña nueva" type="password"
                  autoComplete="new-password" value={confirmation} onChange={(event) => setConfirmation(event.target.value)}
                  error={fieldErrors.confirmation || undefined} disabled={isSaving} />
              </div>
              <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
                <Button type="button" variant="outline" className="min-h-[48px]" disabled={isSaving}
                  onClick={() => confirmLeave(clearForm)}>
                  Descartar
                </Button>
                <Button type="submit" className="min-h-[48px]" isLoading={isSaving} disabled={isSaving}>
                  Guardar contraseña
                </Button>
              </div>
            </form>
          </section>
        </div>
      </div>
      {discardDialog}
    </div>
  )
}
