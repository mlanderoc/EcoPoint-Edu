/**
 * Address of an institution in Valledupar, typed with the street nomenclature
 * instead of free text (professor's review, point 2): type of road, numbers,
 * letters, "Bis", neighbourhood and detail. The same rules are checked by
 * InstitutionValidator in the backend.
 *
 * @author craulgomez
 */

import { Input, Select } from '@/components/ui'

export const CITY = 'Valledupar, Cesar'
export const ROAD_TYPES = ['Calle', 'Carrera', 'Transversal', 'Diagonal', 'Avenida', 'Kilómetro'] as const
const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const PLACE = /^[A-Za-zÁÉÍÓÚÑÜáéíóúñü0-9 .]{1,60}$/

export interface AddressParts {
  road: string
  number: string
  letter: string
  bis: boolean
  cross: string
  crossLetter: string
  plate: string
  destination: string
  neighbourhood: string
  detail: string
}

export const EMPTY_ADDRESS: AddressParts = {
  road: '', number: '', letter: '', bis: false, cross: '', crossLetter: '', plate: '', destination: '',
  neighbourhood: '', detail: '',
}

type AddressErrors = Partial<Record<keyof AddressParts, string>>

function wholeNumber(text: string, max: number, label: string): string | undefined {
  if (!text.trim()) return `${label} es obligatorio.`
  const value = Number(text)
  return Number.isInteger(value) && value >= 1 && value <= max ? undefined : `${label} debe estar entre 1 y ${max}.`
}

function place(text: string, label: string, required: boolean): string | undefined {
  const value = text.trim()
  if (!value) return required ? `${label} es obligatorio.` : undefined
  return PLACE.test(value) ? undefined : `${label}: solo letras, números, espacios y puntos (máx. 60).`
}

const isRoad = (parts: AddressParts) => parts.road === 'Kilómetro'

/** Errors of each field; an empty object when the address is complete and logical. */
export function addressErrors(parts: AddressParts): AddressErrors {
  const errors: AddressErrors = {}
  if (!parts.road) errors.road = 'Escoja el tipo de vía.'
  if (isRoad(parts)) {
    errors.number = wholeNumber(parts.number, 30, 'El kilómetro')
    errors.destination = place(parts.destination, 'El destino de la vía', true)
  } else if (parts.road) {
    errors.number = wholeNumber(parts.number, 150, 'El número de la vía')
    errors.cross = wholeNumber(parts.cross, 150, 'El número después del #')
    errors.plate = wholeNumber(parts.plate, 199, 'El número de la placa')
  }
  errors.neighbourhood = place(parts.neighbourhood, 'El barrio', false)
  errors.detail = place(parts.detail, 'El complemento', false)
  return Object.fromEntries(Object.entries(errors).filter(([, message]) => message)) as AddressErrors
}

/** Address as it is saved, such as "Calle 16A # 12B - 45, Novalito, Valledupar, Cesar". */
export function composeAddress(parts: AddressParts): string {
  const head = isRoad(parts)
    ? `Kilómetro ${Number(parts.number)} vía a ${parts.destination.trim()}`
    : `${parts.road} ${Number(parts.number)}${parts.letter}${parts.bis ? ' Bis' : ''} # ${Number(parts.cross)}${parts.crossLetter} - ${Number(parts.plate)}`
  const extras = [parts.neighbourhood, parts.detail].map((text) => text.trim()).filter(Boolean)
  return [head, ...extras, CITY].join(', ')
}

export interface AddressFieldsProps {
  value: AddressParts
  onChange: (value: AddressParts) => void
  /** Show the errors only after the first attempt to save. */
  showErrors: boolean
  disabled?: boolean
}

export function AddressFields({ value, onChange, showErrors, disabled }: AddressFieldsProps) {
  const errors = showErrors ? addressErrors(value) : {}
  const set = (changes: Partial<AddressParts>) => onChange({ ...value, ...changes })
  const complete = Object.keys(addressErrors(value)).length === 0
  const letterOptions = [{ value: '', label: 'Sin letra' }, ...LETTERS.map((letter) => ({ value: letter, label: letter }))]

  return (
    <fieldset className="address-fields" disabled={disabled}>
      <legend>Dirección en Valledupar</legend>
      <Select id="address-road" label="Tipo de vía" value={value.road} placeholder="Escoja el tipo de vía"
        error={errors.road} onChange={(event) => set({ road: event.target.value })}
        options={ROAD_TYPES.map((road) => ({ value: road, label: road }))} />

      {isRoad(value) ? (
        <div className="address-row">
          <Input id="address-number" label="Kilómetro" inputMode="numeric" placeholder="Ej. 5" value={value.number}
            error={errors.number} onChange={(event) => set({ number: event.target.value.replace(/\D/g, '') })} />
          <Input id="address-destination" label="Vía a" placeholder="Ej. La Paz" value={value.destination}
            error={errors.destination} maxLength={60} onChange={(event) => set({ destination: event.target.value })} />
        </div>
      ) : value.road ? (
        <>
          <div className="address-row">
            <Input id="address-number" label={`Número de la ${value.road.toLowerCase()}`} inputMode="numeric"
              placeholder="Ej. 16" value={value.number} error={errors.number}
              onChange={(event) => set({ number: event.target.value.replace(/\D/g, '') })} />
            <Select id="address-letter" label="Letra" value={value.letter} options={letterOptions}
              onChange={(event) => set({ letter: event.target.value })} />
            <label className="address-bis">
              <input type="checkbox" checked={value.bis} onChange={(event) => set({ bis: event.target.checked })} />
              Bis
            </label>
          </div>
          <div className="address-row">
            <Input id="address-cross" label="#" inputMode="numeric" placeholder="Ej. 12" value={value.cross}
              error={errors.cross} onChange={(event) => set({ cross: event.target.value.replace(/\D/g, '') })} />
            <Select id="address-cross-letter" label="Letra" value={value.crossLetter} options={letterOptions}
              onChange={(event) => set({ crossLetter: event.target.value })} />
            <Input id="address-plate" label="-" inputMode="numeric" placeholder="Ej. 45" value={value.plate}
              error={errors.plate} onChange={(event) => set({ plate: event.target.value.replace(/\D/g, '') })} />
          </div>
        </>
      ) : null}

      <div className="address-row">
        <Input id="address-neighbourhood" label="Barrio (opcional)" placeholder="Ej. Novalito" value={value.neighbourhood}
          error={errors.neighbourhood} maxLength={60} onChange={(event) => set({ neighbourhood: event.target.value })} />
        <Input id="address-detail" label="Complemento (opcional)" placeholder="Ej. Sede principal" value={value.detail}
          error={errors.detail} maxLength={60} onChange={(event) => set({ detail: event.target.value })} />
      </div>
      <p className="address-preview" data-testid="address-preview">
        {complete ? composeAddress(value) : `Complete la dirección. Ciudad: ${CITY}.`}
      </p>
    </fieldset>
  )
}
