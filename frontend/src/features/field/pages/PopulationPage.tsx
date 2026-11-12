/**
 * Population of a shift with its total calculated while typing (Spec 004,
 * T318; R-342, RN-022).
 *
 * @author craulgomez
 */

import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AlertBanner, Button, Card, CardContent, Input } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { fieldService } from '../services/fieldService'

function count(text: string): number {
  const value = Number(text)
  return Number.isInteger(value) && value >= 0 ? value : 0
}

export function PopulationPage() {
  const { id } = useParams()
  const shiftId = Number(id)
  const navigate = useNavigate()
  const [students, setStudents] = useState('')
  const [teachers, setTeachers] = useState('')
  const [staff, setStaff] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const total = count(students) + count(teachers) + count(staff)

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    if (total <= 0) {
      setError('La población del turno debe ser mayor que cero: registre cuántas personas hay presentes.')
      return
    }
    try {
      setIsSaving(true)
      await fieldService.registerPopulation(shiftId, count(students), count(teachers), count(staff))
      navigate(`/campo/turno/${shiftId}`)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo registrar la población.')
      setIsSaving(false)
    }
  }

  const field = (fieldId: string, label: string, value: string, onChange: (text: string) => void) => (
    <Input id={fieldId} label={label} type="number" inputMode="numeric" min={0} step={1} value={value}
      onChange={(event) => onChange(event.target.value)} disabled={isSaving} className="min-h-[56px] text-xl" />
  )

  return (
    <div className="max-w-xl w-full mx-auto flex flex-col gap-5">
      <h1 className="text-2xl font-black text-[var(--color-brand-dark)]">Población del turno</h1>
      {error && <AlertBanner variant="danger" message={error} />}
      <Card>
        <CardContent className="py-5">
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {field('population-students', 'Estudiantes presentes', students, setStudents)}
            {field('population-teachers', 'Docentes', teachers, setTeachers)}
            {field('population-staff', 'Personal administrativo y de servicios', staff, setStaff)}
            <p className="text-lg" data-testid="population-total">
              Total población: <strong className="text-2xl">{total}</strong>
            </p>
            <Button type="submit" className="min-h-[56px] text-lg" isLoading={isSaving} disabled={isSaving}>
              Guardar población
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
