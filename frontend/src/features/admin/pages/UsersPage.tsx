/**
 * Users administration page (Spec 004, T312 / R-322 / CA-302).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Users, Plus, CheckCircle, XCircle, GraduationCap, Award } from 'lucide-react'
import { Button, Card, CardHeader, CardTitle, CardContent, Input, Select, Modal, AlertBanner } from '@/components/ui'
import { User, UserRole } from '@/types'
import { ApiError } from '@/services/apiClient'
import { adminService } from '../services/adminService'

/** Shortest initial password the API accepts (spec 003, UserValidator). */
const MIN_PASSWORD_LENGTH = 8

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'DOCENTE', label: 'Docente Responsable' },
  { value: 'ESTUDIANTE_UNIVERSITARIO', label: 'Estudiante Universitario' },
]

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<UserRole>('DOCENTE')
  const [password, setPassword] = useState('')
  const [emailError, setEmailError] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const loadUsers = async () => {
    try {
      setIsLoading(true)
      const data = await adminService.getUsers()
      setUsers(data)
    } catch {
      setGeneralError('Error al cargar la lista de usuarios.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  const resetForm = () => {
    setFirstName('')
    setLastName('')
    setEmail('')
    setRole('DOCENTE')
    setPassword('')
    setEmailError(null)
    setPasswordError(null)
    setGeneralError(null)
  }

  const handleOpenModal = () => {
    resetForm()
    setIsModalOpen(true)
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setEmailError(null)
    setPasswordError(null)
    setGeneralError(null)

    if (!firstName.trim()) {
      setGeneralError('El nombre del usuario es obligatorio.')
      return
    }
    if (!lastName.trim()) {
      setGeneralError('El apellido del usuario es obligatorio.')
      return
    }
    if (!email.trim()) {
      setEmailError('El correo electrónico es obligatorio.')
      return
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      setPasswordError(`La contraseña inicial debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres.`)
      return
    }

    try {
      setIsSaving(true)
      const created = await adminService.createUser({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim().toLowerCase(),
        role,
        password,
      })
      setUsers((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        // RN-084 (unique email) is the only business rule of a user's data, so a 422
        // is shown next to the email field (CA-302).
        const emailMessage = err.getFieldError('email') ?? (err.status === 422 ? err.message : undefined)
        const passwordMessage = err.getFieldError('password')
        if (emailMessage) {
          setEmailError(emailMessage)
        } else if (passwordMessage) {
          setPasswordError(passwordMessage)
        } else {
          setGeneralError(err.message)
        }
      } else if (err instanceof Error) {
        setGeneralError(err.message)
      } else {
        setGeneralError('Error al registrar el usuario.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">
            Usuarios del Sistema
          </h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Gestión de docentes responsables y estudiantes universitarios participantes en la caracterización.
          </p>
        </div>
        <Button
          variant="primary"
          size="default"
          className="min-h-[48px]"
          onClick={handleOpenModal}
        >
          <Plus className="w-5 h-5 mr-1.5" />
          Nuevo Usuario
        </Button>
      </div>

      {generalError && (
        <AlertBanner
          variant="danger"
          title="Atención"
          message={generalError}
          onDismiss={() => setGeneralError(null)}
        />
      )}

      {/* Users List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {isLoading ? (
          <div className="col-span-full py-12 flex justify-center text-sm font-medium text-[var(--color-text-muted)]">
            Cargando usuarios...
          </div>
        ) : users.length === 0 ? (
          <div className="col-span-full py-12 flex flex-col items-center justify-center text-center p-6 border-2 border-dashed rounded-xl bg-slate-50 border-slate-200">
            <Users className="w-12 h-12 text-slate-400 mb-3" />
            <h3 className="text-base font-bold text-[var(--color-text-main)]">
              No hay usuarios registrados
            </h3>
            <p className="text-sm text-[var(--color-text-muted)] mt-1">
              Haga clic en &quot;Nuevo Usuario&quot; para registrar docentes o estudiantes.
            </p>
          </div>
        ) : (
          users.map((u) => {
            const isDocente = u.role === 'DOCENTE'
            return (
              <Card key={u.id} className="border-slate-200 hover:shadow-md transition-shadow">
                <CardHeader className="flex flex-row items-start justify-between gap-3 pb-3">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-10 h-10 rounded-full flex items-center justify-center font-black text-sm ${
                        isDocente
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {isDocente ? (
                        <Award className="w-5 h-5" />
                      ) : (
                        <GraduationCap className="w-5 h-5" />
                      )}
                    </div>
                    <div>
                      <CardTitle className="text-base">
                        {u.firstName} {u.lastName}
                      </CardTitle>
                      <p className="text-xs text-[var(--color-text-muted)] mt-0.5">{u.email}</p>
                    </div>
                  </div>
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold ${
                      isDocente
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-blue-50 text-blue-700 border border-blue-200'
                    }`}
                  >
                    {isDocente ? 'Docente' : 'Estudiante'}
                  </span>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="flex items-center justify-between text-xs text-[var(--color-text-muted)] pt-2 border-t border-slate-100">
                    <span className="flex items-center gap-1">
                      {u.active ? (
                        <>
                          <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                          <span className="text-emerald-700 font-medium">Activo</span>
                        </>
                      ) : (
                        <>
                          <XCircle className="w-3.5 h-3.5 text-slate-400" />
                          <span>Inactivo</span>
                        </>
                      )}
                    </span>
                    <span>ID: {u.id}</span>
                  </div>
                </CardContent>
              </Card>
            )
          })
        )}
      </div>

      {/* Modal for Creating User */}
      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Registrar Nuevo Usuario"
        description="Agregue un docente responsable o estudiante universitario al sistema."
      >
        <form onSubmit={handleCreate} className="flex flex-col gap-4 mt-2">
          <Input
            id="user-firstname"
            label="Nombres"
            placeholder="Ej. Carlos Raúl"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            disabled={isSaving}
            required
          />

          <Input
            id="user-lastname"
            label="Apellidos"
            placeholder="Ej. Gómez García"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            disabled={isSaving}
            required
          />

          <Input
            id="user-email"
            label="Correo Institucional"
            type="email"
            placeholder="Ej. usuario@unicesar.edu.co"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              if (emailError) setEmailError(null)
            }}
            error={emailError || undefined}
            disabled={isSaving}
            required
          />

          <Select
            id="user-role"
            label="Rol en el Sistema"
            value={role}
            onChange={(e) => setRole(e.target.value as UserRole)}
            options={ROLE_OPTIONS}
            disabled={isSaving}
          />

          <Input
            id="user-password"
            label="Contraseña inicial"
            type="password"
            autoComplete="new-password"
            placeholder={`Mínimo ${MIN_PASSWORD_LENGTH} caracteres`}
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (passwordError) setPasswordError(null)
            }}
            error={passwordError || undefined}
            disabled={isSaving}
            required
          />

          <div className="flex items-center justify-end gap-3 mt-4 pt-4 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              size="default"
              className="min-h-[48px]"
              onClick={() => setIsModalOpen(false)}
              disabled={isSaving}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="default"
              className="min-h-[48px]"
              disabled={isSaving}
            >
              {isSaving ? 'Guardando...' : 'Guardar Usuario'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
