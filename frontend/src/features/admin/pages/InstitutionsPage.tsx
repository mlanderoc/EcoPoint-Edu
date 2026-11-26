/**
 * Institutions administration page: list with the active/inactive filter,
 * activate or deactivate, and create an institution active or inactive
 * (Spec 004, T311; R-320, R-325, R-308; RN-006).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Plus, MapPin } from 'lucide-react'
import {
  ActiveFilter,
  AlertBanner,
  Button,
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
import { AddressFields, AddressParts, EMPTY_ADDRESS, addressErrors, composeAddress } from '../components/AddressFields'
import { RokkoTip } from '@/components/guide/RokkoTip'

function isActive(institution: Institution): boolean {
  return institution.active !== false
}

export function InstitutionsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [address, setAddress] = useState<AddressParts>(EMPTY_ADDRESS)
  const [triedSave, setTriedSave] = useState(false)
  const [active, setActive] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [filter, setFilter] = useActiveFilter()
  const { confirmLeave, discardDialog } = useUnsavedChanges(isModalOpen && !isSaving
    && (name !== '' || JSON.stringify(address) !== JSON.stringify(EMPTY_ADDRESS)))

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
    setAddress(EMPTY_ADDRESS)
    setTriedSave(false)
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
    setTriedSave(true)
    if (Object.keys(addressErrors(address)).length > 0) {
      setError('Revise la dirección: debe ser una dirección válida de Valledupar.')
      return
    }
    try {
      setIsSaving(true)
      setError(null)
      const created = await adminService.createInstitution({ name: name.trim(), address: composeAddress(address), active })
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
    <div className="max-w-6xl w-full mx-auto flex flex-col gap-6">
      <div className="page-hero">
        <div>
          <h1 className="page-hero-title">Instituciones Educativas</h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Gestión de planteles educativos participantes en el muestreo de caracterización.
          </p>
        </div>
        <Button variant="primary" className="min-h-[48px]" onClick={() => { setError(null); setIsModalOpen(true) }}>
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Institución
        </Button>
      </div>

      <RokkoTip tip="institutions" />
      {error && <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />}

      <ActiveFilter value={filter} onChange={setFilter} inactiveCount={inactiveCount} />

      <div className="grid-2">
        {isLoading ? (
          <p className="text-sm text-[var(--color-text-muted)]">Cargando instituciones...</p>
        ) : visible.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No hay instituciones con este filtro.</p>
        ) : (
          visible.map((inst) => (
            <article key={inst.id} className={cn('inst-card', !isActive(inst) && 'opacity-60')}
              data-testid={`institution-${inst.id}`}>
              <div className="panel-head" style={{ alignItems: 'center' }}>
                <h3>{inst.name}</h3>
                <StatusToggleButton active={isActive(inst)} itemName={inst.name} onToggle={() => handleToggle(inst)}
                  disabled={togglingId === inst.id} />
              </div>
              <span className="flex items-center gap-1.5 text-sm text-[var(--color-text-muted)]">
                <MapPin className="w-4 h-4 shrink-0" aria-hidden="true" />{inst.address}
              </span>
              {inst.settings && (
                <div className="chips">
                  <span>Submuestra {inst.settings.minSampleWeight} a {inst.settings.maxSampleWeight} kg</span>
                  <span>Tolerancia {inst.settings.qualityTolerancePercent} %</span>
                  <span>Bolsa máx. {inst.settings.maxBagNetWeight} kg</span>
                </div>
              )}
            </article>
          ))
        )}
      </div>

      <Modal isOpen={isModalOpen} onClose={closeModal} title="Registrar Institución Educativa"
        description="Ingrese los datos de la institución para el estudio participativo.">
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <Input id="institution-name" label="Nombre de la Institución" placeholder="Ej. I.E. Prudencia Daza"
            value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
          <AddressFields value={address} onChange={setAddress} showErrors={triedSave} disabled={isSaving} />
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
