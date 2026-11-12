/**
 * Generation sources administration page (Spec 004, T311 / R-321).
 *
 * @author craulgomez
 */

import { useEffect, useState } from 'react'
import { Plus, CheckCircle, XCircle } from 'lucide-react'
import { Button, Card, CardHeader, CardTitle, CardContent, Input, Select, Modal, AlertBanner } from '@/components/ui'
import { GenerationSource, SourceType } from '@/types'
import { adminService } from '../services/adminService'

const SOURCE_TYPES: { value: SourceType; label: string }[] = [
  { value: 'AULAS', label: 'Aulas de Clase' },
  { value: 'OFICINAS', label: 'Oficinas Administrativas' },
  { value: 'BANOS', label: 'Baterías Sanitarias' },
  { value: 'CAFETERIA', label: 'Cafetería / Tienda / PAE' },
  { value: 'PATIO', label: 'Patios' },
  { value: 'ZONAS_VERDES', label: 'Zonas Verdes' },
  { value: 'LABORATORIO', label: 'Laboratorios' },
  { value: 'OTRO', label: 'Otro' },
]

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

  const loadSources = async () => {
    try {
      setIsLoading(true)
      const data = await adminService.getSources(1)
      setSources(data)
    } catch {
      setError('Error al cargar las fuentes de generación.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadSources()
  }, [])

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
      const created = await adminService.createSource(1, {
        institutionId: 1,
        code: code.trim().toUpperCase(),
        name: name.trim(),
        type,
        active,
      })
      setSources((prev) => [...prev, created])
      setIsModalOpen(false)
      setCode('')
      setName('')
      setType('AULAS')
      setActive(true)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al registrar la fuente.'
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
            Fuentes de Generación
          </h1>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            Puntos ecológicos y áreas físicas de generación de residuos en la institución (R-321).
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
          Nueva Fuente
        </Button>
      </div>

      {error && (
        <AlertBanner variant="danger" title="Atención" message={error} onDismiss={() => setError(null)} />
      )}

      {/* Sources Table / Cards */}
      <Card>
        <CardHeader>
          <CardTitle>Fuentes Registradas (I.E. Prudencia Daza)</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-[var(--color-text-muted)]">Cargando fuentes...</p>
          ) : sources.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">No hay fuentes registradas.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs font-bold text-[var(--color-text-muted)] uppercase tracking-wider">
                    <th className="py-3 px-3">Código</th>
                    <th className="py-3 px-3">Nombre</th>
                    <th className="py-3 px-3">Tipo</th>
                    <th className="py-3 px-3">Estado</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 font-medium">
                  {sources.map((s) => (
                    <tr key={s.id} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3 px-3">
                        <span className="font-mono font-black text-xs px-2.5 py-1 rounded-md bg-[var(--color-primary-light)] text-[var(--color-primary)] border border-emerald-200">
                          {s.code}
                        </span>
                      </td>
                      <td className="py-3 px-3 font-semibold text-slate-800">{s.name}</td>
                      <td className="py-3 px-3 text-slate-600">{s.type}</td>
                      <td className="py-3 px-3">
                        {s.active ? (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                            <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                            Activo
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                            <XCircle className="w-3.5 h-3.5 text-slate-400" />
                            Inactivo
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Modal Nueva Fuente */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Crear Fuente de Generación"
        description="Registre un punto o espacio generador de residuos en el plantel."
      >
        <form onSubmit={handleCreate} className="flex flex-col gap-4">
          <Input
            label="Código de Fuente (ej. AU, BA, OF)"
            placeholder="Ej. AU"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            required
            autoFocus
          />
          <Input
            label="Nombre Descriptivo"
            placeholder="Ej. Aulas Bloque 2"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <Select
            label="Tipo de Espacio"
            value={type}
            onChange={(e) => setType(e.target.value as SourceType)}
            options={SOURCE_TYPES}
          />
          <label className="flex items-center gap-2.5 cursor-pointer select-none text-sm font-semibold text-slate-700 pt-1">
            <input
              type="checkbox"
              checked={active}
              onChange={(e) => setActive(e.target.checked)}
              className="w-4 h-4 rounded border-slate-300 text-[var(--color-primary)] focus:ring-[var(--color-primary)]"
            />
            Fuente activa para muestreo
          </label>
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
              Guardar Fuente
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
