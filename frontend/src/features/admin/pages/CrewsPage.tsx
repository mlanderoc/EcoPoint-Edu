/**
 * Crews administration page: create and edit crews. A student can only be
 * added if he has no other open study (Spec 004, T313 / R-323 / RN-085;
 * RN-090, D15).
 *
 * @author craulgomez
 */

import { useEffect, useMemo, useState } from 'react'
import { Plus, AlertTriangle, Users2 } from 'lucide-react'
import { Button, Input, Select, Modal, AlertBanner } from '@/components/ui'
import { studyService } from '@/features/study/services/studyService'
import { ApiError } from '@/services/apiClient'
import { CharacterizationStudy, Crew, User } from '@/types'
import { adminService } from '../services/adminService'

const initials = (user: User) => `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()

export function CrewsPage() {
  const [crews, setCrews] = useState<Crew[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [runningStudies, setRunningStudies] = useState<CharacterizationStudy[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editing, setEditing] = useState<Crew | null>(null)
  const [busyMembers, setBusyMembers] = useState<Map<number, number>>(new Map())
  const [name, setName] = useState('')
  const [responsibleId, setResponsibleId] = useState<string>('')
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([])
  const [error, setError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    Promise.all([adminService.getCrews(), adminService.getUsers()])
      .then(([crewsData, usersData]) => {
        setCrews(crewsData)
        setUsers(usersData)
      })
      .catch(() => setError('Error al cargar la información de cuadrillas y usuarios.'))
      .finally(() => setIsLoading(false))
    studyService.fieldStudies().then(setRunningStudies).catch(() => setRunningStudies([]))
  }, [])

  const teachers = users.filter((u) => u.role === 'DOCENTE' && u.active)
  const students = useMemo(() => users.filter((u) => u.role === 'ESTUDIANTE_UNIVERSITARIO'
    && (u.active || editing?.memberIds.includes(u.id))), [users, editing])

  const openForm = (crew: Crew | null) => {
    setEditing(crew)
    setName(crew?.name ?? '')
    setResponsibleId(crew?.responsibleId ? String(crew.responsibleId) : teachers.length > 0 ? String(teachers[0].id) : '')
    setSelectedMemberIds(crew?.memberIds ?? [])
    setFormError(null)
    setBusyMembers(new Map())
    setIsModalOpen(true)
    adminService.getBusyMembers(crew?.id)
      .then((busy) => setBusyMembers(new Map(busy.map((item) => [item.userId, item.studyId]))))
      .catch(() => setFormError('No se pudo comprobar qué estudiantes están libres.'))
  }

  const handleMemberToggle = (studentId: number) => {
    setSelectedMemberIds((prev) =>
      prev.includes(studentId)
        ? prev.filter((id) => id !== studentId)
        : [...prev, studentId]
    )
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)

    if (!name.trim()) {
      setFormError('El nombre de la cuadrilla es obligatorio.')
      return
    }

    if (!responsibleId || Number(responsibleId) <= 0) {
      setFormError('Toda cuadrilla debe tener un docente responsable.')
      return
    }

    const data = { name: name.trim(), responsibleId: Number(responsibleId), memberIds: selectedMemberIds }
    try {
      setIsSaving(true)
      if (editing) {
        const updated = await adminService.updateCrew(editing.id, data)
        setCrews((prev) => prev.map((crew) => (crew.id === updated.id ? updated : crew)))
      } else {
        const created = await adminService.createCrew(data)
        setCrews((prev) => [...prev, created])
      }
      setIsModalOpen(false)
    } catch (err: unknown) {
      setFormError(err instanceof ApiError || err instanceof Error ? err.message : 'Error al guardar la cuadrilla.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-4">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Cuadrillas de Muestreo</h1>
          <p className="m-0 mt-1">Equipos que registran en campo.</p>
        </div>
        <Button variant="primary" size="default" className="min-h-[48px]" onClick={() => openForm(null)}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Cuadrilla
        </Button>
      </div>

      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

      <div className="grid-2">
        {isLoading ? (
          <p className="text-sm text-[var(--color-text-muted)]">Cargando cuadrillas...</p>
        ) : crews.length === 0 ? (
          <div className="panel empty" style={{ gridColumn: '1 / -1' }}>
            <Users2 className="w-10 h-10 text-slate-400" aria-hidden="true" />
            <h3>No hay cuadrillas registradas</h3>
            <p className="m-0 text-sm text-[var(--color-text-muted)]">
              Haga clic en &quot;Nueva Cuadrilla&quot; para conformar los equipos de muestreo.
            </p>
          </div>
        ) : (
          crews.map((crew) => {
            const responsible = users.find((u) => u.id === crew.responsibleId)
            const hasResponsible = Boolean(responsible && responsible.role === 'DOCENTE')
            const members = users.filter((u) => crew.memberIds.includes(u.id))
            const running = runningStudies.filter((study) => study.crewId === crew.id).map((study) => `N.º ${study.id}`)
            return (
              <article key={crew.id} className="inst-card">
                <div className="panel-head">
                  <h3>{crew.name}</h3>
                  <span className="pill">{crew.memberIds.length} integrantes</span>
                </div>
                {!hasResponsible && (
                  <AlertBanner variant="warning" title="Cuadrilla sin docente responsable"
                    message="Esta cuadrilla no cuenta con un docente responsable asignado. Toda cuadrilla requiere un docente para operar en turnos." />
                )}
                <div className="data-list">
                  {hasResponsible && (
                    <div><span>Responsable</span><b>{responsible?.firstName} {responsible?.lastName}</b></div>
                  )}
                  <div><span>En ejecución</span><b>{running.length > 0 ? `Estudio ${running.join(', ')}` : 'Sin estudio'}</b></div>
                </div>
                {members.length === 0 ? (
                  <p className="m-0 text-sm text-[var(--color-text-muted)]">Sin estudiantes asignados.</p>
                ) : (
                  <div className="members">
                    {members.map((member) => (
                      <span key={member.id} className="member">
                        <span className="avatar">{initials(member)}</span>
                        {member.firstName} {member.lastName}
                      </span>
                    ))}
                  </div>
                )}
                <div className="card-foot">
                  <span />
                  <Button variant="outline" size="sm" aria-label={`Editar ${crew.name}`} onClick={() => openForm(crew)}>
                    Editar
                  </Button>
                </div>
              </article>
            )
          })
        )}
      </div>

      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editing ? `Editar ${editing.name}` : 'Crear Cuadrilla de Muestreo'}
        description="Solo se pueden agregar estudiantes libres, sin otro estudio asignado."
      >
        <form onSubmit={handleSave} className="flex flex-col gap-4 mt-2">
          {formError && <AlertBanner variant="danger" message={formError} onDismiss={() => setFormError(null)} />}
          <Input
            id="crew-name"
            label="Nombre de la Cuadrilla"
            placeholder="Ej. Cuadrilla Mañana A"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={isSaving}
            required
          />

          <div>
            <Select
              id="crew-responsible"
              label="Docente responsable"
              value={responsibleId}
              onChange={(e) => setResponsibleId(e.target.value)}
              disabled={isSaving}
              options={[
                { value: '', label: '-- Seleccione un docente responsable --' },
                ...teachers.map((t) => ({
                  value: String(t.id),
                  label: `${t.firstName} ${t.lastName} (${t.email})`,
                })),
              ]}
            />
            {(!responsibleId || Number(responsibleId) <= 0) && (
              <p role="alert" className="text-xs font-semibold text-amber-700 mt-1 flex items-center gap-1">
                <AlertTriangle className="w-3.5 h-3.5" />
                Es obligatorio asignar un docente responsable.
              </p>
            )}
          </div>

          <fieldset className="m-0 border-0 p-0">
            <legend className="text-sm font-bold text-[var(--color-text-main)] mb-1.5">Estudiantes Integrantes</legend>
            <div className="max-h-56 overflow-y-auto rounded-xl bg-[var(--soft)] p-2 flex flex-col gap-1">
              {students.length === 0 ? (
                <p className="m-0 p-2 text-xs text-[var(--color-text-muted)]">No hay estudiantes activos disponibles.</p>
              ) : (
                students.map((student) => {
                  const studyId = busyMembers.get(student.id)
                  const isMember = selectedMemberIds.includes(student.id)
                  const blocked = studyId !== undefined && !editing?.memberIds.includes(student.id)
                  return (
                    <label key={student.id}
                      className={`flex items-center gap-2 min-h-[44px] p-2 rounded-lg text-sm font-medium ${blocked ? 'opacity-60' : 'hover:bg-white cursor-pointer'}`}>
                      <input
                        type="checkbox"
                        checked={isMember}
                        onChange={() => handleMemberToggle(student.id)}
                        disabled={isSaving || blocked}
                        className="w-4 h-4 accent-[var(--brand)]"
                      />
                      <span className="min-w-0 flex-1">
                        {student.firstName} {student.lastName}{' '}
                        <span className="text-[var(--color-text-muted)]">({student.email})</span>
                      </span>
                      {blocked && <span className="pill warm">En Estudio N.º {studyId}</span>}
                    </label>
                  )
                })
              )}
            </div>
          </fieldset>

          <div className="flex items-center justify-end gap-3 pt-2">
            <Button type="button" variant="outline" size="default" className="min-h-[48px]"
              onClick={() => setIsModalOpen(false)} disabled={isSaving}>
              Cancelar
            </Button>
            <Button type="submit" variant="primary" size="default" className="min-h-[48px]"
              disabled={isSaving || !responsibleId || Number(responsibleId) <= 0}>
              {isSaving ? 'Guardando...' : 'Guardar Cuadrilla'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
