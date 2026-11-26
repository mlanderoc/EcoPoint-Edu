/**
 * Generation sources administration page: list with the active/inactive
 * filter, activate or deactivate, and create a source active or inactive
 * (Spec 004, T311; R-321, R-325, R-308, CA-312).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import {
  ActiveFilter,
  AlertBanner,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Modal,
  Select,
  StatusToggleButton,
  matchesActiveFilter,
  useActiveFilter,
} from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { GenerationSource, SourceType } from '@/types'
import { cn } from '@/utils/cn'
import { adminService } from '../services/adminService'
import { InstitutionSelect, useInstitutionParam, useInstitutions } from '../components/InstitutionSelect'
import { RokkoTip } from '@/components/guide/RokkoTip'

const SOURCE_TYPES: { value: SourceType; label: string }[] = [
  { value: 'AULAS', label: 'Aulas' },
  { value: 'OFICINAS', label: 'Oficinas' },
  { value: 'TIENDA_ESCOLAR', label: 'Tienda escolar' },
  { value: 'PAE', label: 'Programa de Alimentación Escolar (PAE)' },
  { value: 'ZONAS_VERDES', label: 'Zonas verdes' },
  { value: 'BANOS', label: 'Baños' },
  { value: 'LABORATORIO', label: 'Laboratorio' },
  { value: 'OTRA', label: 'Otra' },
]

const TYPE_LABELS = Object.fromEntries(SOURCE_TYPES.map((type) => [type.value, type.label])) as Record<SourceType, string>

export function SourcesPage() {
  const [sources, setSources] = useState<GenerationSource[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [type, setType] = useState<SourceType | ''>('')
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [filter, setFilter] = useActiveFilter()
  const { institutions } = useInstitutions(false)
  const [institutionId, setInstitutionId] = useInstitutionParam(institutions)
  const { confirmLeave, discardDialog } = useUnsavedChanges(isModalOpen && !isSaving && (name + type) !== '')

  useEffect(() => {
    if (institutionId === null) return
    setIsLoading(true)
    adminService.getSources(institutionId)
      .then(setSources)
      .catch(() => setError('Error al cargar las fuentes de generación.'))
      .finally(() => setIsLoading(false))
  }, [institutionId])

  const visible = sources.filter((source) => matchesActiveFilter(source.active, filter))
  const inactiveCount = sources.filter((source) => !source.active).length

  const resetForm = () => {
    setName('')
    setType('')
    setActive(true)
  }

  const closeModal = () => confirmLeave(() => {
    setIsModalOpen(false)
    resetForm()
  })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      setError('El nombre de la fuente es obligatorio.')
      return
    }
    if (!type) {
      setError('Escoja el tipo de espacio de la fuente.')
      return
    }
    if (institutionId === null) {
      setError('Elija la institución de la fuente.')
      return
    }
    try {
      setIsSaving(true)
      setError(null)
      const created = await adminService.createSource(institutionId, {
        institutionId,
        name: name.trim(),
        type,
        active,
      })
      setSources((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al registrar la fuente.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleToggle = async (source: GenerationSource) => {
    if (institutionId === null) return
    try {
      setTogglingId(source.id)
      setError(null)
      const updated = await adminService.setSourceActive(institutionId, source.id, !source.active)
      setSources((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado de la fuente.')
    } finally {
      setTogglingId(null)
    }
  }

  return (
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-6">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Fuentes de Generación</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Puntos ecológicos y áreas físicas de generación de residuos en la institución.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={() => { setError(null); setIsModalOpen(true) }}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Fuente
        </Button>
      </div>

      <RokkoTip tip="sources" />
      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

      <div className="sm:max-w-md">
        <InstitutionSelect institutions={institutions} value={institutionId} onChange={setInstitutionId} />
      </div>

      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <CardTitle>Fuentes registradas</CardTitle>
          <ActiveFilter value={filter} onChange={setFilter} inactiveCount={inactiveCount} />
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-[var(--color-text-muted)]">Cargando fuentes...</p>
          ) : visible.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">No hay fuentes con este filtro.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th className="py-3 px-2">Código</th>
                    <th className="py-3 px-2">Nombre</th>
                    <th className="py-3 px-2 hidden sm:table-cell">Tipo</th>
                    <th className="py-3 px-2"><span className="sr-only">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((s) => (
                    <tr key={s.id} className={cn('hover:bg-slate-50 transition-colors', !s.active && 'opacity-60')}
                      data-testid={`source-row-${s.code}`}>
                      <td className="py-3 px-2">
                        <span className="code">
                          {s.code}
                        </span>
                      </td>
                      <td className="py-3 px-2 font-semibold text-slate-800">{s.name}</td>
                      <td className="py-3 px-2 text-slate-600 hidden sm:table-cell">{TYPE_LABELS[s.type] ?? s.type}</td>
                      <td className="py-3 px-2 text-right">
                        <StatusToggleButton active={s.active} itemName={s.code} onToggle={() => handleToggle(s)}
                          disabled={togglingId === s.id} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <Modal isOpen={isModalOpen} onClose={closeModal} title="Crear Fuente de Generación"
        description="Registre un punto o espacio generador de residuos en el plantel.">
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <Input id="source-name" label="Nombre Descriptivo" placeholder="Ej. Aulas Bloque 2" value={name}
            onChange={(e) => setName(e.target.value)} required autoFocus maxLength={80} />
          <Select id="source-type" label="Tipo de Espacio" value={type} placeholder="Escoja el tipo de espacio"
            onChange={(e) => setType(e.target.value as SourceType)} options={SOURCE_TYPES} />
          <p className="m-0 text-xs text-[var(--muted)]" data-testid="source-code-note">
            El código de la fuente se asigna solo a partir del nombre (por ejemplo, AB2 para «Aulas Bloque 2»).
          </p>
          <label className="flex items-center gap-2.5 select-none text-sm font-semibold text-slate-700 pt-1">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)}
              className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]" />
            Fuente activa para muestreo
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" className="min-h-[48px]" onClick={closeModal} disabled={isSaving}>
              Descartar
            </Button>
            <Button type="submit" variant="primary" className="min-h-[48px]" loading={isSaving}>
              Guardar Fuente
            </Button>
          </div>
        </form>
      </Modal>
      {discardDialog}
    </div>
  )
}
