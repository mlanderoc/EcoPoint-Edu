/**
 * Users administration page: a table filtered and paginated on the server, with
 * the totals of records of each user, activate or deactivate students, and the
 * form to create a user (Spec 004, T312; R-322, R-308, CA-302, CA-313; RN-087).
 *
 * @author craulgomez
 */

import { useCallback, useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, Plus, Search } from 'lucide-react'
import { AlertBanner, Button, Card, CardContent, Input, Modal, Select, StatusToggleButton } from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { ApiError } from '@/services/apiClient'
import { UserRole, UserRow } from '@/types'
import { cn } from '@/utils/cn'
import { adminService } from '../services/adminService'

/** Shortest initial password the API accepts (spec 003, UserValidator). */
const MIN_PASSWORD_LENGTH = 8
/** Rows per page of the table (plan D13, decision T14). */
export const USERS_PAGE_SIZE = 20
const SEARCH_DELAY_MS = 300

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'DOCENTE', label: 'Docente Responsable' },
  { value: 'ESTUDIANTE_UNIVERSITARIO', label: 'Estudiante Universitario' },
]

const ROLE_FILTER = [
  { value: '', label: 'Todos los roles' },
  { value: 'ESTUDIANTE_UNIVERSITARIO', label: 'Estudiantes' },
  { value: 'DOCENTE', label: 'Docentes' },
]

const STATE_FILTER = [
  { value: '', label: 'Activos e inactivos' },
  { value: 'true', label: 'Solo activos' },
  { value: 'false', label: 'Solo inactivos' },
]

export function UsersPage() {
  const [rows, setRows] = useState<UserRow[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [pageNumber, setPageNumber] = useState(0)
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('')
  const [stateFilter, setStateFilter] = useState('')
  const [text, setText] = useState('')
  const [searchText, setSearchText] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [togglingId, setTogglingId] = useState<number | null>(null)
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
  const { confirmLeave, discardDialog } = useUnsavedChanges(
    isModalOpen && !isSaving && (firstName + lastName + email + password) !== '')

  const loadUsers = useCallback(async () => {
    try {
      setIsLoading(true)
      const result = await adminService.listUsers({
        role: roleFilter,
        active: stateFilter === '' ? undefined : stateFilter === 'true',
        text: searchText,
        page: pageNumber,
        size: USERS_PAGE_SIZE,
      })
      setRows(result.items)
      setTotalItems(result.totalItems)
    } catch {
      setGeneralError('Error al cargar la lista de usuarios.')
    } finally {
      setIsLoading(false)
    }
  }, [roleFilter, stateFilter, searchText, pageNumber])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchText(text)
      setPageNumber(0)
    }, SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [text])

  const pageCount = Math.max(1, Math.ceil(totalItems / USERS_PAGE_SIZE))

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

  const closeModal = () => confirmLeave(() => {
    setIsModalOpen(false)
    resetForm()
  })

  const handleToggle = async (row: UserRow) => {
    try {
      setTogglingId(row.id)
      setGeneralError(null)
      const updated = await adminService.setUserActive(row.id, !row.active)
      setRows((previous) => previous.map((item) => (item.id === updated.id ? { ...item, active: updated.active } : item)))
    } catch (err: unknown) {
      setGeneralError(err instanceof Error ? err.message : 'No se pudo cambiar el estado del usuario.')
    } finally {
      setTogglingId(null)
    }
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
      await adminService.createUser({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim().toLowerCase(),
        role,
        password,
      })
      setIsModalOpen(false)
      resetForm()
      await loadUsers()
    } catch (err: unknown) {
      if (err instanceof ApiError) {
        // The unique email is the only business rule of a user's data, so a 422
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
      } else {
        setGeneralError(err instanceof Error ? err.message : 'Error al registrar el usuario.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Usuarios del Sistema</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Gestión de docentes responsables y estudiantes universitarios participantes en la caracterización.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={handleOpenModal}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nuevo Usuario
        </Button>
      </div>

      {generalError && !isModalOpen && (
        <AlertBanner variant="danger" title="Atención" message={generalError} onDismiss={() => setGeneralError(null)} />
      )}

      <Card>
        <CardContent className="py-4 flex flex-col gap-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-[42px] pointer-events-none" aria-hidden="true" />
              <Input id="users-search" label="Buscar" placeholder="Nombre o correo" value={text}
                onChange={(e) => setText(e.target.value)} className="pl-9" />
            </div>
            <Select id="users-role" label="Rol" value={roleFilter} options={ROLE_FILTER}
              onChange={(e) => { setRoleFilter(e.target.value as UserRole | ''); setPageNumber(0) }} />
            <Select id="users-state" label="Estado" value={stateFilter} options={STATE_FILTER}
              onChange={(e) => { setStateFilter(e.target.value); setPageNumber(0) }} />
          </div>

          {isLoading && rows.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">Cargando usuarios...</p>
          ) : rows.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">No hay usuarios con estos filtros.</p>
          ) : (
            <table className="w-full text-left text-sm" data-testid="users-table">
              <thead>
                <tr className="border-b border-slate-200 text-xs font-bold text-[var(--color-text-muted)] uppercase tracking-wider">
                  <th className="py-3 px-2">Usuario</th>
                  <th className="py-3 px-2 hidden sm:table-cell">Rol</th>
                  <th className="py-3 px-2 hidden sm:table-cell">Estado</th>
                  <th className="py-3 px-2 hidden md:table-cell text-right">Registros</th>
                  <th className="py-3 px-2 text-right"><span className="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((u) => {
                  const isTeacher = u.role === 'DOCENTE'
                  return (
                    <tr key={u.id} className={cn('align-middle', !u.active && 'opacity-60')} data-testid={`user-row-${u.id}`}>
                      <td className="py-3 px-2 min-w-0">
                        <p className="font-semibold text-slate-800 break-words">{u.firstName} {u.lastName}</p>
                        <p className="text-xs text-[var(--color-text-muted)] break-all">{u.email}</p>
                        <p className="text-xs text-[var(--color-text-muted)] sm:hidden mt-1">
                          {isTeacher ? 'Rol docente' : 'Rol estudiante'} · {u.active ? 'activo' : 'inactivo'} ·{' '}
                          {u.activity.typedRecords} registros
                        </p>
                      </td>
                      <td className="py-3 px-2 hidden sm:table-cell">
                        <span className={cn('inline-flex px-2.5 py-1 rounded-full text-xs font-bold border',
                          isTeacher ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : 'bg-blue-50 text-blue-700 border-blue-200')}>
                          {isTeacher ? 'Docente' : 'Estudiante'}
                        </span>
                      </td>
                      <td className="py-3 px-2 hidden sm:table-cell">
                        <span className={cn('text-xs font-bold', u.active ? 'text-emerald-700' : 'text-slate-500')}>
                          {u.active ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="py-3 px-2 hidden md:table-cell text-right tabular-nums"
                        title={`${u.activity.bags} bolsas · ${u.activity.verifications} verificaciones`}>
                        {u.activity.typedRecords}
                      </td>
                      <td className="py-3 px-2 text-right">
                        {isTeacher ? (
                          <span className="text-xs text-[var(--color-text-muted)]">—</span>
                        ) : (
                          <StatusToggleButton active={u.active} itemName={`${u.firstName} ${u.lastName}`}
                            onToggle={() => handleToggle(u)} disabled={togglingId === u.id} />
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}

          <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
            <p className="text-[var(--color-text-muted)]" data-testid="users-total">
              {totalItems} {totalItems === 1 ? 'usuario' : 'usuarios'} · página {pageNumber + 1} de {pageCount}
            </p>
            <div className="flex gap-2">
              <Button variant="outline" className="min-h-[40px]" disabled={pageNumber === 0 || isLoading}
                onClick={() => setPageNumber((current) => current - 1)}>
                <ChevronLeft className="w-4 h-4 mr-1" aria-hidden="true" /> Anterior
              </Button>
              <Button variant="outline" className="min-h-[40px]" disabled={pageNumber + 1 >= pageCount || isLoading}
                onClick={() => setPageNumber((current) => current + 1)}>
                Siguiente <ChevronRight className="w-4 h-4 ml-1" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Modal open={isModalOpen} onClose={closeModal} title="Registrar Nuevo Usuario"
        description="Agregue un docente responsable o estudiante universitario al sistema.">
        <form onSubmit={handleCreate} className="flex flex-col gap-4 mt-2">
          {generalError && <AlertBanner variant="danger" message={generalError} />}
          <Input id="user-firstname" label="Nombres" placeholder="Ej. Carlos Raúl" value={firstName}
            onChange={(e) => setFirstName(e.target.value)} disabled={isSaving} required />
          <Input id="user-lastname" label="Apellidos" placeholder="Ej. Gómez García" value={lastName}
            onChange={(e) => setLastName(e.target.value)} disabled={isSaving} required />
          <Input id="user-email" label="Correo Institucional" type="email" placeholder="Ej. usuario@unicesar.edu.co"
            value={email} onChange={(e) => { setEmail(e.target.value); if (emailError) setEmailError(null) }}
            error={emailError || undefined} disabled={isSaving} required />
          <Select id="user-role" label="Rol en el Sistema" value={role}
            onChange={(e) => setRole(e.target.value as UserRole)} options={ROLE_OPTIONS} disabled={isSaving} />
          <Input id="user-password" label="Contraseña inicial" type="password" autoComplete="new-password"
            placeholder={`Mínimo ${MIN_PASSWORD_LENGTH} caracteres`} value={password}
            onChange={(e) => { setPassword(e.target.value); if (passwordError) setPasswordError(null) }}
            error={passwordError || undefined} disabled={isSaving} required />
          <div className="flex items-center justify-end gap-3 mt-4 pt-4 border-t border-slate-100">
            <Button type="button" variant="outline" className="min-h-[48px]" onClick={closeModal} disabled={isSaving}>
              Descartar
            </Button>
            <Button type="submit" variant="primary" className="min-h-[48px]" disabled={isSaving}>
              {isSaving ? 'Guardando...' : 'Guardar Usuario'}
            </Button>
          </div>
        </form>
      </Modal>
      {discardDialog}
    </div>
  )
}
