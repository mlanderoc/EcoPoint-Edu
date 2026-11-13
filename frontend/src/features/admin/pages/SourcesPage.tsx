/**
 * Generation sources administration page: list with the active/inactive
 * filter, activate or deactivate, and create a source active or inactive
 * (Spec 004, T311; R-321, R-325, R-308, CA-312).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Plus, CheckCircle, XCircle } from 'lucide-react'
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

const INSTITUTION_ID = 1

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
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [type, setType] = useState<SourceType>('AULAS')
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [filter, setFilter] = useActiveFilter()
  const { confirmLeave, discardDialog } = useUnsavedChanges(isModalOpen && !isSaving && (code + name) !== '')

  useEffect(() => {
    adminService.getSources(INSTITUTION_ID)
      .then(setSources)
      .catch(() => setError('Error al cargar las fuentes de generación.'))
      .finally(() => setIsLoading(false))
  }, [])

  const visible = sources.filter((source) => matchesActiveFilter(source.active, filter))
  const inactiveCount = sources.filter((source) => !source.active).length

  const resetForm = () => {
    setCode('')
    setName('')
    setType('AULAS')
    setActive(true)
  }

  const closeModal = () => confirmLeave(() => {
    setIsModalOpen(false)
    resetForm()
  })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!code.trim()) {
      setError('El código de la fuente es obligatorio (ej. AU).')
      return
    }
    if (!name.trim()) {
      setError('El nombre de la fuente es obligatorio.')
      return
    }
    try {
      setIsSaving(true)
      setError(null)
      const created = await adminService.createSource(INSTITUTION_ID, {
        institutionId: INSTITUTION_ID,
        code: code.trim().toUpperCase(),
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
    try {
      setTogglingId(source.id)
      setError(null)
      const updated = await adminService.setSourceActive(INSTITUTION_ID, source.id, !source.active)
      setSources((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado de la fuente.')
    } finally {
      setTogglingId(null)
    }
  }

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Fuentes de Generación</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Puntos ecológicos y áreas físicas de generación de residuos en la institución.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={() => { setError(null); setIsModalOpen(true) }}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Fuente
        </Button>
      </div>

      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

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
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs font-bold text-[var(--color-text-muted)] uppercase tracking-wider">
                    <th className="py-3 px-3">Código</th>
                    <th className="py-3 px-3">Nombre</th>
                    <th className="py-3 px-3 hidden sm:table-cell">Tipo</th>
                    <th className="py-3 px-3">Estado</th>
                    <th className="py-3 px-3"><span className="sr-only">Acciones</span></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-medium">
                  {visible.map((s) => (
                    <tr key={s.id} className={cn('hover:bg-slate-50 transition-colors', !s.active && 'opacity-60')}
                      data-testid={`source-row-${s.code}`}>
                      <td className="py-3 px-3">
                        <span className="font-mono font-black text-xs px-2.5 py-1 rounded-md bg-[var(--color-primary-light)] text-[var(--color-primary)] border border-emerald-200">
                          {s.code}
                        </span>
                      </td>
                      <td className="py-3 px-3 font-semibold text-slate-800">{s.name}</td>
                      <td className="py-3 px-3 text-slate-600 hidden sm:table-cell">{TYPE_LABELS[s.type] ?? s.type}</td>
                      <td className="py-3 px-3">
                        {s.active ? (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                            <CheckCircle className="w-3.5 h-3.5 text-emerald-600" /> Activo
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                            <XCircle className="w-3.5 h-3.5 text-slate-400" /> Inactivo
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-right">
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
          <Input id="source-code" label="Código de Fuente (ej. AU, BA, OF)" placeholder="Ej. AU" value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())} required autoFocus />
          <Input id="source-name" label="Nombre Descriptivo" placeholder="Ej. Aulas Bloque 2" value={name}
            onChange={(e) => setName(e.target.value)} required />
          <Select id="source-type" label="Tipo de Espacio" value={type}
            onChange={(e) => setType(e.target.value as SourceType)} options={SOURCE_TYPES} />
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
