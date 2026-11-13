/**
 * Waste subcategories administration page (Spec 004, T313 / R-324).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Tag, Plus, CheckCircle, XCircle } from 'lucide-react'
import { Button, Card, CardHeader, CardContent, Input, Select, Modal, AlertBanner } from '@/components/ui'
import { WasteGroupBadge } from '@/components/waste'
import { WasteCategory, WasteGroup } from '@/types'
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

  const loadCategories = async () => {
    try {
      setIsLoading(true)
      const data = await adminService.getCategories()
      setCategories(data)
    } catch {
      setError('Error al cargar las subcategorías de residuos.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadCategories()
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

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('El nombre de la subcategoría es obligatorio.')
      return
    }

    try {
      setIsSaving(true)
      const created = await adminService.createCategory({
        name: name.trim(),
        group,
        active,
      })
      setCategories((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar la subcategoría.'
      setError(msg)
    } finally {
      setIsSaving(false)
    }
  }

  const groupedCategories: Record<WasteGroup, WasteCategory[]> = {
    BLANCO: categories.filter((c) => c.group === 'BLANCO'),
    VERDE: categories.filter((c) => c.group === 'VERDE'),
    NEGRO: categories.filter((c) => c.group === 'NEGRO'),
    SIN_COLOR: categories.filter((c) => c.group === 'SIN_COLOR'),
  }

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">
            Subcategorías de Residuos
          </h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Clasificación oficial de subcategorías según los 4 códigos de color de la Res. 2184/2019.
          </p>
        </div>
        <Button
          variant="primary"
          size="default"
          className="min-h-[48px]"
          onClick={handleOpenModal}
        >
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Subcategoría
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

      {/* Categories by Group */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {isLoading ? (
          <div className="col-span-full py-12 flex justify-center text-sm font-medium text-[var(--color-text-muted)]">
            Cargando subcategorías...
          </div>
        ) : (
          (Object.keys(groupedCategories) as WasteGroup[]).map((grp) => {
            const list = groupedCategories[grp]
            return (
              <Card key={grp} className="border-slate-200">
                <CardHeader className="pb-3 border-b border-slate-100 flex flex-row items-center justify-between gap-2">
                  <WasteGroupBadge group={grp} showDescription size="default" />
                  <span className="text-xs font-bold text-slate-500">
                    {list.length} subcategorías
                  </span>
                </CardHeader>
                <CardContent className="pt-3">
                  {list.length === 0 ? (
                    <p className="text-xs text-slate-400 italic py-2">
                      Sin subcategorías registradas en este grupo.
                    </p>
                  ) : (
                    <div className="flex flex-col divide-y divide-slate-100">
                      {list.map((c) => (
                        <div
                          key={c.id}
                          className="py-2.5 flex items-center justify-between gap-2 text-sm"
                        >
                          <div className="flex items-center gap-2">
                            <Tag className="w-3.5 h-3.5 text-slate-400" />
                            <span className="font-semibold text-slate-800">{c.name}</span>
                          </div>
                          <span className="flex items-center gap-1 text-xs">
                            {c.active ? (
                              <span className="inline-flex items-center gap-1 text-emerald-700 font-medium">
                                <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                                Activa
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 text-slate-400">
                                <XCircle className="w-3.5 h-3.5" />
                                Inactiva
                              </span>
                            )}
                          </span>
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

      {/* Modal for Creating Category */}
      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Registrar Subcategoría de Residuo"
        description="Agregue un nuevo tipo de residuo asignándolo a uno de los 4 grupos de la Res. 2184/2019."
      >
        <form onSubmit={handleCreate} className="flex flex-col gap-4 mt-2">
          <Input
            id="category-name"
            label="Nombre de la Subcategoría"
            placeholder="Ej. Vidrio transparente, Tetra Pak, Pilas AA"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={isSaving}
            required
          />

          <Select
            id="category-group"
            label="Grupo de Color (Res. 2184/2019)"
            value={group}
            onChange={(e) => setGroup(e.target.value as WasteGroup)}
            options={WASTE_GROUPS}
            disabled={isSaving}
          />

          <div className="flex items-center gap-2 pt-2">
            <input
              id="category-active"
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              disabled={isSaving}
              className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
            />
            <label
              htmlFor="category-active"
              className="text-sm font-medium text-[var(--color-text-main)] cursor-pointer"
            >
              Subcategoría activa para registros de cuarteo
            </label>
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
              disabled={isSaving}
            >
              {isSaving ? 'Guardando...' : 'Guardar Subcategoría'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
