/**
 * Institutions administration page (Spec 004, T311 / R-320).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Building, Plus, MapPin } from 'lucide-react'
import { Button, Card, CardHeader, CardTitle, CardContent, Input, Modal, AlertBanner } from '@/components/ui'
import { Institution } from '@/types'
import { adminService } from '../services/adminService'

export function InstitutionsPage() {
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const loadInstitutions = async () => {
    try {
      setIsLoading(true)
      const data = await adminService.getInstitutions()
      setInstitutions(data)
    } catch {
      setError('Error al cargar las instituciones.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadInstitutions()
  }, [])

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
      const created = await adminService.createInstitution({
        name: name.trim(),
        address: address.trim(),
      })
      setInstitutions((prev) => [...prev, created])
      setIsModalOpen(false)
      setName('')
      setAddress('')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar la institución.'
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
            Instituciones Educativas
          </h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Gestión de planteles educativos participantes en el muestreo de caracterización.
          </p>
        </div>
        <Button
          variant="primary"
          size="default"
          className="min-h-[48px]"
          onClick={() => {
            setError(null)
            setIsModalOpen(true)
          }}
        >
          <Plus className="w-5 h-5 mr-1.5" />
          Nueva Institución
        </Button>
      </div>

      {error && (
        <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />
      )}

      {/* Institutions List */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {isLoading ? (
          <p className="text-sm text-[var(--color-text-muted)]">Cargando instituciones...</p>
        ) : institutions.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No hay instituciones registradas.</p>
        ) : (
          institutions.map((inst) => (
            <Card key={inst.id} className="hover:border-slate-300 transition-colors">
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
              <CardContent className="pt-2 flex justify-end">
                <span className="text-xs font-semibold px-2.5 py-1 bg-slate-100 rounded-full text-slate-700">
                  ID: {inst.id}
                </span>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Modal Nueva Institución */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Registrar Institución Educativa"
        description="Ingrese los datos de la institución para el estudio participativo."
      >
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <Input
            label="Nombre de la Institución"
            placeholder="Ej. I.E. Prudencia Daza"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            autoFocus
          />
          <Input
            label="Dirección"
            placeholder="Ej. Calle 16 # 19-32, Valledupar"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            required
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="default"
              className="min-h-[48px]"
              onClick={() => setIsModalOpen(false)}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="default"
              className="min-h-[48px]"
              loading={isSaving}
            >
              Guardar Institución
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
