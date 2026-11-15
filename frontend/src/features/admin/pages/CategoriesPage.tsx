/**
 * Waste subcategories administration page: grouped by color, with the
 * active/inactive filter, activate or deactivate, and create a subcategory
 * active or inactive (Spec 004, T313; R-324, R-325, R-308; RN-043).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Tag, Plus } from 'lucide-react'
import {
  ActiveFilter,
  AlertBanner,
  Button,
  Card,
  CardContent,
  CardHeader,
  Input,
  Modal,
  Select,
  StatusToggleButton,
  matchesActiveFilter,
  useActiveFilter,
} from '@/components/ui'
import { WasteGroupBadge } from '@/components/waste'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { WasteCategory, WasteGroup } from '@/types'
import { cn } from '@/utils/cn'
import { adminService } from '../services/adminService'

const WASTE_GROUPS: { value: WasteGroup; label: string }[] = [
  { value: 'BLANCO', label: 'Blanco (Aprovechables limpios)' },
  { value: 'VERDE', label: 'Verde (Orgánicos biodegradables)' },
  { value: 'NEGRO', label: 'Negro (No aprovechables / Ordinarios)' },
  { value: 'SIN_COLOR', label: 'Sin Color (Peligrosos / RAEE)' },
]

export function CategoriesPage() {
  const [categories, setCategories] = useState<WasteCategory[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [group, setGroup] = useState<WasteGroup>('BLANCO')
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [filter, setFilter] = useActiveFilter()
  const { confirmLeave, discardDialog } = useUnsavedChanges(isModalOpen && !isSaving && name !== '')

  useEffect(() => {
    adminService.getCategories()
      .then(setCategories)
      .catch(() => setError('Error al cargar las subcategorías de residuos.'))
      .finally(() => setIsLoading(false))
  }, [])

  const resetForm = () => {
    setName('')
    setGroup('BLANCO')
    setActive(true)
    setError(null)
  }

  const handleOpenModal = () => {
    resetForm()
    setIsModalOpen(true)
  }

  const closeModal = () => confirmLeave(() => {
    setIsModalOpen(false)
    resetForm()
  })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!name.trim()) {
      setError('El nombre de la subcategoría es obligatorio.')
      return
    }
    try {
      setIsSaving(true)
      const created = await adminService.createCategory({ name: name.trim(), group, active })
      setCategories((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al registrar la subcategoría.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleToggle = async (category: WasteCategory) => {
    try {
      setTogglingId(category.id)
      setError(null)
      const updated = await adminService.setCategoryActive(category.id, !category.active)
      setCategories((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado de la subcategoría.')
    } finally {
      setTogglingId(null)
    }
  }

  const visible = categories.filter((category) => matchesActiveFilter(category.active, filter))
  const inactiveCount = categories.filter((category) => !category.active).length

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Subcategorías de Residuos</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Clasificación oficial de subcategorías según los 4 códigos de color de la Res. 2184/2019.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={handleOpenModal}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Subcategoría
        </Button>
      </div>

      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

      <ActiveFilter value={filter} onChange={setFilter} inactiveCount={inactiveCount} />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {isLoading ? (
          <div className="col-span-full py-12 flex justify-center text-sm font-medium text-[var(--color-text-muted)]">
            Cargando subcategorías...
          </div>
        ) : (
          WASTE_GROUPS.map(({ value: grp }) => {
            const list = visible.filter((category) => category.group === grp)
            return (
              <Card key={grp} className="border-slate-200">
                <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between gap-2">
                  <WasteGroupBadge group={grp} showDescription size="default" />
                  <span className="text-xs font-bold text-slate-500">{list.length} subcategorías</span>
                </CardHeader>
                <CardContent className="pt-3">
                  {list.length === 0 ? (
                    <p className="text-xs text-slate-500 italic py-2">Sin subcategorías con este filtro.</p>
                  ) : (
                    <div className="flex flex-col divide-y divide-slate-100">
                      {list.map((c) => (
                        <div key={c.id} className={cn('py-2.5 flex items-center justify-between gap-2 text-sm',
                          !c.active && 'opacity-60')} data-testid={`category-${c.id}`}>
                          <div className="flex items-center gap-2 min-w-0">
                            <Tag className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                            <span className="font-semibold text-slate-800 truncate">{c.name}</span>
                            {!c.active && (
                              <span className="text-xs font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                                Inactiva
                              </span>
                            )}
                          </div>
                          <StatusToggleButton active={c.active} itemName={c.name} onToggle={() => handleToggle(c)}
                            disabled={togglingId === c.id} />
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            )
          })
        )}
      </div>

      <Modal open={isModalOpen} onClose={closeModal} title="Registrar Subcategoría de Residuo"
        description="Agregue un nuevo tipo de residuo asignándolo a uno de los 4 grupos de la Res. 2184/2019.">
        <form onSubmit={handleCreate} className="flex flex-col gap-4 mt-2">
          <Input id="category-name" label="Nombre de la Subcategoría"
            placeholder="Ej. Vidrio transparente, Tetra Pak, Pilas AA" value={name}
            onChange={(e) => setName(e.target.value)} disabled={isSaving} required />
          <Select id="category-group" label="Grupo de Color (Res. 2184/2019)" value={group}
            onChange={(e) => setGroup(e.target.value as WasteGroup)} options={WASTE_GROUPS} disabled={isSaving} />
          <div className="flex items-center gap-2 pt-2">
            <input id="category-active" type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)}
              disabled={isSaving}
              className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]" />
            <label htmlFor="category-active" className="text-sm font-medium text-[var(--color-text-main)]">
              Subcategoría activa para registros de cuarteo
            </label>
          </div>
          <div className="flex items-center justify-end gap-3 mt-4 pt-4 border-t border-slate-100">
            <Button type="button" variant="outline" className="min-h-[48px]" onClick={closeModal} disabled={isSaving}>
              Descartar
            </Button>
            <Button type="submit" variant="primary" className="min-h-[48px]" disabled={isSaving}>
              {isSaving ? 'Guardando...' : 'Guardar Subcategoría'}
            </Button>
          </div>
        </form>
      </Modal>
      {discardDialog}
    </div>
  )
}
