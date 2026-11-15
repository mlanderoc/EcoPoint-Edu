/**
 * Institutions administration page: list with the active/inactive filter,
 * activate or deactivate, and create an institution active or inactive
 * (Spec 004, T311; R-320, R-325, R-308; RN-006).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Building, Plus, MapPin } from 'lucide-react'
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
  StatusToggleButton,
  matchesActiveFilter,
  useActiveFilter,
} from '@/components/ui'
import { useUnsavedChanges } from '@/hooks/useUnsavedChanges'
import { Institution } from '@/types'
import { cn } from '@/utils/cn'
import { adminService } from '../services/adminService'

function isActive(institution: Institution): boolean {
  return institution.active !== false
}

export function InstitutionsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [filter, setFilter] = useActiveFilter()
  const { confirmLeave, discardDialog } = useUnsavedChanges(isModalOpen && !isSaving && (name + address) !== '')

  useEffect(() => {
    adminService.getInstitutions()
      .then(setInstitutions)
      .catch(() => setError('Error al cargar las instituciones.'))
      .finally(() => setIsLoading(false))
  }, [])

  const visible = institutions.filter((institution) => matchesActiveFilter(isActive(institution), filter))
  const inactiveCount = institutions.filter((institution) => !isActive(institution)).length

  const resetForm = () => {
    setName('')
    setAddress('')
    setActive(true)
  }

  const closeModal = () => confirmLeave(() => {
    setIsModalOpen(false)
    resetForm()
  })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      setError('El nombre de la institución es obligatorio.')
      return
    }
    if (!address.trim()) {
      setError('La dirección de la institución es obligatoria.')
      return
    }
    try {
      setIsSaving(true)
      setError(null)
      const created = await adminService.createInstitution({ name: name.trim(), address: address.trim(), active })
      setInstitutions((prev) => [...prev, created])
      setIsModalOpen(false)
      resetForm()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Error al registrar la institución.')
    } finally {
      setIsSaving(false)
    }
  }

  const handleToggle = async (institution: Institution) => {
    try {
      setTogglingId(institution.id)
      setError(null)
      const updated = await adminService.setInstitutionActive(institution.id, !isActive(institution))
      setInstitutions((prev) => prev.map((item) => (item.id === updated.id ? updated : item)))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado de la institución.')
    } finally {
      setTogglingId(null)
    }
  }

  return (
    <div className="max-w-5xl w-full mx-auto flex flex-col gap-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Instituciones Educativas</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Gestión de planteles educativos participantes en el muestreo de caracterización.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={() => { setError(null); setIsModalOpen(true) }}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Institución
        </Button>
      </div>

      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

      <ActiveFilter value={filter} onChange={setFilter} inactiveCount={inactiveCount} />

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {isLoading ? (
          <p className="text-sm text-[var(--color-text-muted)]">Cargando instituciones...</p>
        ) : visible.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No hay instituciones con este filtro.</p>
        ) : (
          visible.map((inst) => (
            <Card key={inst.id} className={cn('hover:border-slate-300 transition-colors', !isActive(inst) && 'opacity-60')}
              data-testid={`institution-${inst.id}`}>
              <CardHeader className="pb-2">
                <div className="flex items-start gap-3">
                  <div className="p-3 bg-[var(--color-primary-light)] rounded-xl text-[var(--color-primary)]">
                    <Building className="w-6 h-6" />
                  </div>
                  <div>
                    <CardTitle className="text-lg">{inst.name}</CardTitle>
                    <div className="flex items-center gap-1.5 text-xs text-[var(--color-text-muted)] mt-1">
                      <MapPin className="w-4 h-4 text-slate-400 shrink-0" />
                      <span>{inst.address}</span>
                    </div>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="pt-2 flex items-center justify-between gap-2">
                <span className={cn('text-xs font-bold px-2.5 py-1 rounded-full',
                  isActive(inst) ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500')}>
                  {isActive(inst) ? 'Activa' : 'Inactiva'}
                </span>
                <StatusToggleButton active={isActive(inst)} itemName={inst.name} onToggle={() => handleToggle(inst)}
                  disabled={togglingId === inst.id} />
              </CardContent>
            </Card>
          ))
        )}
      </div>

      <Modal isOpen={isModalOpen} onClose={closeModal} title="Registrar Institución Educativa"
        description="Ingrese los datos de la institución para el estudio participativo.">
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <Input id="institution-name" label="Nombre de la Institución" placeholder="Ej. I.E. Prudencia Daza"
            value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
          <Input id="institution-address" label="Dirección" placeholder="Ej. Calle 16 # 19-32, Valledupar"
            value={address} onChange={(e) => setAddress(e.target.value)} required />
          <label className="flex items-center gap-2.5 select-none text-sm font-semibold text-slate-700 pt-1">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)}
              className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]" />
            Institución activa para estudios nuevos
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" className="min-h-[48px]" onClick={closeModal} disabled={isSaving}>
              Descartar
            </Button>
            <Button type="submit" variant="primary" className="min-h-[48px]" loading={isSaving}>
              Guardar Institución
            </Button>
          </div>
        </form>
      </Modal>
      {discardDialog}
    </div>
  )
}
