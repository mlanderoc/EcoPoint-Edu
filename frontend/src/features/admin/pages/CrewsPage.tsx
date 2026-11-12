/**
 * Crews administration page (Spec 004, T313 / R-323 / RN-085).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Users2, Plus, UserCheck, AlertTriangle, GraduationCap } from 'lucide-react'
import { Button, Card, CardHeader, CardTitle, CardContent, Input, Select, Modal, AlertBanner } from '@/components/ui'
import { Crew, User } from '@/types'
import { adminService } from '../services/adminService'

export function CrewsPage() {
  const [crews, setCrews] = useState<Crew[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [responsibleId, setResponsibleId] = useState<string>('')
  const [selectedMemberIds, setSelectedMemberIds] = useState<number[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const loadData = async () => {
    try {
      setIsLoading(true)
      const [crewsData, usersData] = await Promise.all([
        adminService.getCrews(),
        adminService.getUsers(),
      ])
      setCrews(crewsData)
      setUsers(usersData)
    } catch {
      setError('Error al cargar la información de cuadrillas y usuarios.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const teachers = users.filter((u) => u.role === 'DOCENTE' && u.active)
  const students = users.filter((u) => u.role === 'ESTUDIANTE_UNIVERSITARIO' && u.active)

  const resetForm = () => {
    setName('')
    setResponsibleId(teachers.length > 0 ? String(teachers[0].id) : '')
    setSelectedMemberIds([])
    setError(null)
  }

  const handleOpenModal = () => {
    resetForm()
    setIsModalOpen(true)
  }

  const handleMemberToggle = (studentId: number) => {
    setSelectedMemberIds((prev) =>
      prev.includes(studentId)
        ? prev.filter((id) => id !== studentId)
        : [...prev, studentId]
    )
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('El nombre de la cuadrilla es obligatorio.')
      return
    }

    if (!responsibleId || Number(responsibleId) <= 0) {
      setError('Alerta Preventiva RN-085: Toda cuadrilla debe tener obligatoriamente un docente responsable.')
      return
    }

    try {
      setIsSaving(true)
      const created = await adminService.createCrew({
        name: name.trim(),
        responsibleId: Number(responsibleId),
        memberIds: selectedMemberIds,
      })
      setCrews((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar la cuadrilla.'
      setError(msg)
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
            Cuadrillas de Muestreo
          </h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Equipos de trabajo de campo conformados por un docente responsable y estudiantes universitarios (R-323, RN-085).
          </p>
        </div>
        <Button
          variant="primary"
          size="default"
          className="min-h-[48px]"
          onClick={handleOpenModal}
        >
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Cuadrilla
        </Button>
      </div>

      {error && (
        <AlertBanner
          variant="danger"
          title="Atención"
          message={error}
          onDismiss={() => setError(null)}
        />
      )}

      {/* Crews List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {isLoading ? (
          <div className="col-span-full py-12 flex justify-center text-sm font-medium text-[var(--color-text-muted)]">
            Cargando cuadrillas...
          </div>
        ) : crews.length === 0 ? (
          <div className="col-span-full py-12 flex flex-col items-center justify-center text-center p-6 border-2 border-dashed rounded-xl bg-slate-50 border-slate-200">
            <Users2 className="w-12 h-12 text-slate-400 mb-3" />
            <h3 className="text-base font-bold text-[var(--color-text-main)]">
              No hay cuadrillas registradas
            </h3>
            <p className="text-sm text-[var(--color-text-muted)] mt-1">
              Haga clic en &quot;Nueva Cuadrilla&quot; para conformar los equipos de muestreo.
            </p>
          </div>
        ) : (
          crews.map((crew) => {
            const responsible = users.find((u) => u.id === crew.responsibleId)
            const hasResponsible = Boolean(responsible && responsible.role === 'DOCENTE')
            const members = users.filter((u) => crew.memberIds.includes(u.id))

            return (
              <Card key={crew.id} className="border-slate-200 hover:shadow-md transition-shadow">
                <CardHeader className="pb-3">
                  <div className="flex items-center justify-between gap-2">
                    <CardTitle className="text-base font-bold">{crew.name}</CardTitle>
                    <span className="text-xs text-[var(--color-text-muted)] font-mono">
                      ID: {crew.id}
                    </span>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-3">
                  {/* Responsible Teacher Section */}
                  {hasResponsible ? (
                    <div className="flex items-center gap-2 p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 text-xs text-emerald-900">
                      <UserCheck className="w-4 h-4 text-emerald-700 shrink-0" />
                      <div>
                        <p className="font-bold text-[11px] text-emerald-800 uppercase tracking-wide">
                          Docente Responsable
                        </p>
                        <p className="font-semibold text-emerald-950">
                          {responsible?.firstName} {responsible?.lastName}
                        </p>
                      </div>
                    </div>
                  ) : (
                    /* RN-085 Preventive Warning Banner */
                    <AlertBanner
                      variant="warning"
                      title="Alerta Preventiva RN-085"
                      message="Esta cuadrilla no cuenta con un docente responsable asignado. Toda cuadrilla requiere un docente para operar en turnos."
                    />
                  )}

                  {/* Student Members Section */}
                  <div>
                    <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700 mb-1.5">
                      <GraduationCap className="w-3.5 h-3.5 text-slate-500" />
                      <span>Estudiantes Integrantes ({members.length})</span>
                    </div>
                    {members.length === 0 ? (
                      <p className="text-xs text-slate-400 italic">Sin estudiantes asignados.</p>
                    ) : (
                      <div className="flex flex-wrap gap-1.5">
                        {members.map((m) => (
                          <span
                            key={m.id}
                            className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-slate-100 text-slate-800 border border-slate-200"
                          >
                            {m.firstName} {m.lastName}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            )
          })
        )}
      </div>

      {/* Modal for Creating Crew */}
      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Crear Cuadrilla de Muestreo"
        description="Asigne un nombre, docente responsable (obligatorio según RN-085) y estudiantes participantes."
      >
        <form onSubmit={handleCreate} className="flex flex-col gap-4 mt-2">
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
              label="Docente Responsable (RN-085)"
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
                Alerta RN-085: Es obligatorio asignar un docente responsable.
              </p>
            )}
          </div>

          <div>
            <label className="text-sm font-bold text-[var(--color-text-main)] block mb-1.5">
              Estudiantes Integrantes
            </label>
            <div className="max-h-48 overflow-y-auto border border-slate-200 rounded-lg p-2.5 flex flex-col gap-2 bg-slate-50/50">
              {students.length === 0 ? (
                <p className="text-xs text-slate-400 italic">No hay estudiantes activos disponibles.</p>
              ) : (
                students.map((st) => (
                  <label
                    key={st.id}
                    className="flex items-center gap-2 p-2 rounded-md hover:bg-white cursor-pointer transition-colors text-xs font-medium text-slate-800"
                  >
                    <input
                      type="checkbox"
                      checked={selectedMemberIds.includes(st.id)}
                      onChange={() => handleMemberToggle(st.id)}
                      disabled={isSaving}
                      className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
                    />
                    <span>
                      {st.firstName} {st.lastName}{' '}
                      <span className="text-slate-400">({st.email})</span>
                    </span>
                  </label>
                ))
              )}
            </div>
          </div>

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
              disabled={isSaving || !responsibleId || Number(responsibleId) <= 0}
            >
              {isSaving ? 'Guardando...' : 'Guardar Cuadrilla'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
