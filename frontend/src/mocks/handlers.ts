/**
 * Mock of the REST API of Spec 003 for development without backend and for the
 * tests. Routes, JSON and error format follow the real API (plan of Spec 003,
 * sections 4 and 5); turn it off with VITE_USE_MOCK_API=false.
 *
 * @author craulgomez
 */

import { http, HttpResponse } from 'msw'
import {
  MockShift,
  mockBags,
  mockCategories,
  mockCrews,
  mockInstitution,
  mockShifts,
  mockSources,
  mockStudy,
  mockUsers,
} from './data/seedData'
import {
  Alert,
  CharacterizationStudy,
  CorrectionEntry,
  Crew,
  RecordField,
  VerifiableRecord,
  DensityMeasurement,
  Quartering,
  WasteGroup,
  GenerationSource,
  Institution,
  SamplingShift,
  ShiftSlot,
  ShiftType,
  SourceProgress,
  StudyStatus,
  User,
  UserRole,
  WasteBag,
  WasteCategory,
} from '@/types'
import { ALL_PERMISSIONS, hasPermission } from '@/features/auth/utils/permissions'

/** Demo password of every seed user (sql/002_seed.sql). */
const DEMO_PASSWORD = 'EcoPoint2026'
const NEXT_STATUS: Record<StudyStatus, StudyStatus | null> = {
  PLANIFICADO: 'EN_EJECUCION',
  EN_EJECUCION: 'EN_DEPURACION',
  EN_DEPURACION: 'CERRADO',
  CERRADO: null,
  CANCELADO: null,
}

let activeUser: User | null = mockUsers[0]
let institutionsState: Institution[] = [{ ...mockInstitution }]
let usersState: User[] = [...mockUsers]
let sourcesState: GenerationSource[] = [...mockSources]
let crewsState: Crew[] = [...mockCrews]
let categoriesState: WasteCategory[] = [...mockCategories]
let studiesState: CharacterizationStudy[] = [{ ...mockStudy }]
let shiftsState: MockShift[] = cloneShifts()
let bagsState: WasteBag[] = structuredClone(mockBags)
let quarteringsState: Quartering[] = []
let densitiesState: DensityMeasurement[] = []
let alertsState: Alert[] = []
let correctionsState: { type: string; id: number; entry: CorrectionEntry }[] = []
let nextId = 1000

function newId(): number {
  nextId += 1
  return nextId
}

function apiError(status: number, errorCode: string, message: string, path: string,
                  fieldErrors: { field: string; message: string }[] = []) {
  return HttpResponse.json(
    { timestamp: new Date().toISOString(), status, errorCode, message, path, fieldErrors },
    { status }
  )
}

function notFound(path: string, what: string) {
  return apiError(404, 'ERR_NOT_FOUND', `El recurso ${what} no fue encontrado en el sistema.`, path)
}

function requireSession(path: string) {
  return activeUser ? null : apiError(401, 'ERR_UNAUTHORIZED', 'Debe iniciar sesión para continuar.', path)
}

function requirePermission(permission: Parameters<typeof hasPermission>[1], path: string) {
  const noSession = requireSession(path)
  if (noSession) {
    return noSession
  }
  return hasPermission(activeUser?.role, permission)
    ? null
    : apiError(403, 'ERR_FORBIDDEN', 'No tiene permiso para realizar esta acción.', path)
}

function withSession(user: User): User {
  const institution = institutionsState.find((item) => item.id === user.institutionId)
  return {
    ...user,
    institutionName: institution?.name ?? null,
    permissions: ALL_PERMISSIONS.filter((permission) => hasPermission(user.role, permission)),
  }
}

function round2(value: number): number {
  return Math.round(value * 100) / 100
}

function participatingSources(studyId: number): GenerationSource[] {
  const study = studiesState.find((item) => item.id === studyId)
  const ids = new Set(study?.participatingSourceIds ?? [])
  return sourcesState.filter((source) => ids.has(source.id))
}

function progressOf(shift: MockShift): SourceProgress[] {
  return participatingSources(shift.studyId).map((source) => {
    const bags = bagsState.filter((bag) => bag.shiftId === shift.id && bag.sourceId === source.id)
    const status = bags.length > 0 ? 'CON_GENERACION'
      : shift.sourcesWithoutGeneration.includes(source.id) ? 'SIN_GENERACION' : 'PENDIENTE'
    return {
      sourceId: source.id,
      code: source.code,
      name: source.name,
      type: source.type,
      bagCount: bags.length,
      totalNetWeight: round2(bags.reduce((sum, bag) => sum + bag.netWeight, 0)),
      status,
    }
  })
}

function shiftCode(dayNumber: number, type: ShiftType): string {
  return `D${dayNumber}-${type === 'MANANA' ? 'M' : 'T'}`
}

function toShiftResponse(shift: MockShift): SamplingShift {
  const bags = bagsState.filter((bag) => bag.shiftId === shift.id)
  return {
    ...shift,
    code: shiftCode(shift.dayNumber, shift.type),
    bagCount: bags.length,
    totalNetWeight: round2(bags.reduce((sum, bag) => sum + bag.netWeight, 0)),
    sourcesWithoutGeneration: [...shift.sourcesWithoutGeneration].sort((a, b) => a - b),
    hasQuartering: quarteringsState.some((item) => item.shiftId === shift.id),
    hasDensity: densitiesState.some((item) => item.shiftId === shift.id),
    sources: progressOf(shift),
  }
}

function page<T>(items: T[], url: URL) {
  const size = Math.min(Number(url.searchParams.get('size') ?? 50) || 50, 200)
  const pageNumber = Math.max(Number(url.searchParams.get('page') ?? 0) || 0, 0)
  const from = pageNumber * size
  return { items: items.slice(from, from + size), page: pageNumber, size, totalItems: items.length }
}

function findShift(params: Record<string, unknown>): MockShift | undefined {
  return shiftsState.find((shift) => shift.id === Number(params.shiftId))
}

export const handlers = [
  // --- AUTHENTICATION (plan 003, section 4.1) ---
  http.get('/api/v1/auth/csrf', () => new HttpResponse(null, { status: 204 })),

  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email?: string; password?: string }
    const found = usersState.find(
      (user) => user.email.toLowerCase() === body.email?.trim().toLowerCase() && user.active
    )
    if (!found || body.password !== DEMO_PASSWORD) {
      return apiError(401, 'ERR_UNAUTHORIZED', 'Correo o contraseña incorrectos.', '/api/v1/auth/login')
    }
    activeUser = found
    return HttpResponse.json(withSession(found))
  }),

  http.post('/api/v1/auth/logout', () => {
    activeUser = null
    return new HttpResponse(null, { status: 204 })
  }),

  http.get('/api/v1/auth/me', () => {
    const noSession = requireSession('/api/v1/auth/me')
    return noSession ?? HttpResponse.json(withSession(activeUser as User))
  }),

  // --- INSTITUTIONS AND SOURCES (section 5.1) ---
  http.get('/api/v1/institutions', () => HttpResponse.json(institutionsState)),

  http.post('/api/v1/institutions', async ({ request }) => {
    const denied = requirePermission('MANAGE_ADMINISTRATION', '/api/v1/institutions')
    if (denied) return denied
    const body = (await request.json()) as Omit<Institution, 'id'>
    const created: Institution = { mapZoom: 17, ...body, id: newId() }
    institutionsState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.put('/api/v1/institutions/:id', async ({ params, request }) => {
    const index = institutionsState.findIndex((item) => item.id === Number(params.id))
    if (index < 0) return notFound(`/api/v1/institutions/${params.id}`, 'Institución')
    const body = (await request.json()) as Partial<Institution>
    institutionsState[index] = { ...institutionsState[index], ...body, id: institutionsState[index].id }
    return HttpResponse.json(institutionsState[index])
  }),

  http.get('/api/v1/institutions/:id/sources', ({ request }) => {
    const onlyActive = new URL(request.url).searchParams.get('active') === 'true'
    return HttpResponse.json(sourcesState.filter((source) => !onlyActive || source.active))
  }),

  http.post('/api/v1/institutions/:id/sources', async ({ params, request }) => {
    const path = `/api/v1/institutions/${params.id}/sources`
    const denied = requirePermission('MANAGE_ADMINISTRATION', path)
    if (denied) return denied
    const body = (await request.json()) as Pick<GenerationSource, 'code' | 'name' | 'type'>
    if (sourcesState.some((source) => source.code === body.code)) {
      return apiError(422, 'ERR_BUSINESS_RULE', `Ya existe una fuente con el código ${body.code} en esta institución.`, path)
    }
    const created: GenerationSource = {
      ...body, id: newId(), institutionId: Number(params.id) || 1, active: true, hasGeometry: false,
    }
    sourcesState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.put('/api/v1/institutions/:id/sources/:sourceId', async ({ params, request }) => {
    const index = sourcesState.findIndex((source) => source.id === Number(params.sourceId))
    if (index < 0) return notFound(`/api/v1/institutions/${params.id}/sources/${params.sourceId}`, 'Fuente')
    const body = (await request.json()) as Partial<GenerationSource>
    sourcesState[index] = { ...sourcesState[index], ...body, id: sourcesState[index].id }
    return HttpResponse.json(sourcesState[index])
  }),

  http.post('/api/v1/institutions/:id/sources/:sourceId/:action', ({ params }) => {
    const source = sourcesState.find((item) => item.id === Number(params.sourceId))
    if (!source) return notFound(`/api/v1/institutions/${params.id}/sources/${params.sourceId}`, 'Fuente')
    source.active = params.action === 'activate'
    return HttpResponse.json(source)
  }),

  // --- USERS, CREWS AND CATEGORIES (section 5.1) ---
  http.get('/api/v1/users', ({ request }) => {
    const denied = requirePermission('MANAGE_ADMINISTRATION', '/api/v1/users')
    if (denied) return denied
    const url = new URL(request.url)
    const role = url.searchParams.get('role') as UserRole | null
    return HttpResponse.json(page(usersState.filter((user) => !role || user.role === role), url))
  }),

  http.post('/api/v1/users', async ({ request }) => {
    const body = (await request.json()) as Omit<User, 'id' | 'active'> & { password?: string }
    if (usersState.some((user) => user.email.toLowerCase() === body.email.trim().toLowerCase())) {
      return apiError(422, 'ERR_BUSINESS_RULE', `El correo ${body.email.trim()} ya está registrado por otro usuario.`,
        '/api/v1/users')
    }
    const { password: _password, ...data } = body
    const created: User = { ...data, id: newId(), active: true, institutionId: data.institutionId ?? 1 }
    usersState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.get('/api/v1/crews', () => HttpResponse.json(crewsState)),

  http.post('/api/v1/crews', async ({ request }) => {
    const body = (await request.json()) as Omit<Crew, 'id'>
    const responsible = usersState.find((user) => user.id === body.responsibleId)
    if (!responsible || responsible.role !== 'DOCENTE') {
      return apiError(422, 'ERR_BUSINESS_RULE', `La cuadrilla ${body.name} debe tener un docente como responsable.`,
        '/api/v1/crews')
    }
    const created: Crew = {
      ...body, id: newId(), responsibleName: `${responsible.firstName} ${responsible.lastName}`,
    }
    crewsState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.get('/api/v1/waste-categories', ({ request }) => {
    const group = new URL(request.url).searchParams.get('group')
    return HttpResponse.json(categoriesState.filter((category) => !group || category.group === group))
  }),

  http.post('/api/v1/waste-categories', async ({ request }) => {
    const body = (await request.json()) as Omit<WasteCategory, 'id' | 'active'>
    const created: WasteCategory = {
      ...body, id: newId(), active: true, hazardous: body.group === 'SIN_COLOR',
    }
    categoriesState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  // --- STUDIES (section 5.2) ---
  http.get('/api/v1/studies', () => HttpResponse.json(studiesState)),

  http.get('/api/v1/studies/:id', ({ params }) => {
    const study = studiesState.find((item) => item.id === Number(params.id))
    return study ? HttpResponse.json(study) : notFound(`/api/v1/studies/${params.id}`, 'Estudio')
  }),

  http.post('/api/v1/studies', async ({ request }) => {
    const denied = requirePermission('MANAGE_STUDIES', '/api/v1/studies')
    if (denied) return denied
    const body = (await request.json()) as { institutionId?: number; days: { dayNumber: number; date: string | null }[] }
    const days = Array.from({ length: 8 }, (_, index) => ({
      dayNumber: index + 1,
      date: body.days.find((day) => day.dayNumber === index + 1)?.date ?? null,
      discarded: index === 0,
    }))
    const created: CharacterizationStudy = {
      id: newId(),
      institutionId: body.institutionId ?? 1,
      status: 'PLANIFICADO',
      startDate: days[0].date,
      endDate: days[7].date,
      days,
      participatingSourceIds: sourcesState.filter((source) => source.active).map((source) => source.id),
      pendingRecords: 0,
    }
    studiesState.push(created)
    return HttpResponse.json(created, { status: 201 })
  }),

  http.put('/api/v1/studies/:id/days', async ({ params, request }) => {
    const path = `/api/v1/studies/${params.id}/days`
    const denied = requirePermission('MANAGE_STUDIES', path)
    if (denied) return denied
    const study = studiesState.find((item) => item.id === Number(params.id))
    if (!study) return notFound(path, 'Estudio')
    const body = (await request.json()) as { days: { dayNumber: number; date: string | null }[] }
    study.days = study.days.map((day) => ({
      ...day, date: body.days.find((item) => item.dayNumber === day.dayNumber)?.date ?? null,
    }))
    study.startDate = study.days[0].date
    study.endDate = study.days[7].date
    return HttpResponse.json(study)
  }),

  http.post('/api/v1/studies/:id/cancel', async ({ params, request }) => {
    const path = `/api/v1/studies/${params.id}/cancel`
    const denied = requirePermission('MANAGE_STUDIES', path)
    if (denied) return denied
    const study = studiesState.find((item) => item.id === Number(params.id))
    if (!study) return notFound(path, 'Estudio')
    const body = (await request.json()) as { reason?: string }
    if (!body.reason?.trim()) {
      return apiError(400, 'ERR_INVALID_DATA', 'Debe indicar el motivo para cancelar el estudio.', path)
    }
    if (study.status !== 'EN_EJECUCION' && study.status !== 'EN_DEPURACION') {
      return apiError(422, 'ERR_BUSINESS_RULE', 'Solo se puede cancelar un estudio en ejecución o en depuración.', path)
    }
    study.status = 'CANCELADO'
    study.cancellationReason = body.reason.trim()
    study.cancelledAt = new Date().toISOString()
    return HttpResponse.json(study)
  }),

  http.delete('/api/v1/studies/:id', ({ params }) => {
    const path = `/api/v1/studies/${params.id}`
    const denied = requirePermission('MANAGE_STUDIES', path)
    if (denied) return denied
    const study = studiesState.find((item) => item.id === Number(params.id))
    if (!study) return notFound(path, 'Estudio')
    if (study.status !== 'PLANIFICADO') {
      return apiError(422, 'ERR_BUSINESS_RULE', 'Solo se puede eliminar un estudio planificado.', path)
    }
    studiesState = studiesState.filter((item) => item.id !== study.id)
    return new HttpResponse(null, { status: 204 })
  }),

  http.put('/api/v1/studies/:id/sources', async ({ params, request }) => {
    const study = studiesState.find((item) => item.id === Number(params.id))
    if (!study) return notFound(`/api/v1/studies/${params.id}/sources`, 'Estudio')
    const body = (await request.json()) as { sourceIds: number[] }
    study.participatingSourceIds = [...body.sourceIds].sort((a, b) => a - b)
    return HttpResponse.json(study)
  }),

  http.post('/api/v1/studies/:id/status', async ({ params, request }) => {
    const path = `/api/v1/studies/${params.id}/status`
    const denied = requirePermission('MANAGE_STUDIES', path)
    if (denied) return denied
    const study = studiesState.find((item) => item.id === Number(params.id))
    if (!study) return notFound(path, 'Estudio')
    const body = (await request.json()) as { status: StudyStatus; reason?: string }
    if (study.status === 'EN_DEPURACION' && body.status === 'CERRADO' && study.pendingRecords > 0) {
      return apiError(422, 'ERR_BUSINESS_RULE',
        `No se puede cerrar el estudio: quedan ${study.pendingRecords} registros pendientes de verificación.`, path)
    }
    const reopening = study.status === 'CERRADO' && body.status === 'EN_DEPURACION'
    if (!reopening && NEXT_STATUS[study.status] !== body.status) {
      return apiError(422, 'ERR_BUSINESS_RULE', 'El estudio solo avanza un estado a la vez.', path)
    }
    study.status = body.status
    return HttpResponse.json(study)
  }),

  // --- SHIFTS (section 5.2) ---
  http.get('/api/v1/studies/:id/shifts', ({ params }) => {
    const studyId = Number(params.id)
    const total = participatingSources(studyId).length
    const slots: ShiftSlot[] = []
    for (let day = 1; day <= 8; day += 1) {
      for (const type of ['MANANA', 'TARDE'] as ShiftType[]) {
        const shift = shiftsState.find((item) => item.studyId === studyId && item.dayNumber === day && item.type === type)
        const covered = shift ? progressOf(shift).filter((source) => source.status !== 'PENDIENTE').length : 0
        slots.push({
          dayNumber: day, type, shiftId: shift?.id ?? null, status: shift?.status ?? null,
          coveredSources: covered, totalSources: total,
        })
      }
    }
    return HttpResponse.json(slots)
  }),

  http.post('/api/v1/studies/:id/shifts', async ({ params, request }) => {
    const path = `/api/v1/studies/${params.id}/shifts`
    const denied = requirePermission('MANAGE_SHIFTS', path)
    if (denied) return denied
    const body = (await request.json()) as { dayNumber: number; type: ShiftType; crewId?: number | null }
    const studyId = Number(params.id)
    if (shiftsState.some((shift) => shift.studyId === studyId && shift.dayNumber === body.dayNumber
      && shift.type === body.type)) {
      const label = body.type === 'MANANA' ? 'mañana' : 'tarde'
      return apiError(422, 'ERR_BUSINESS_RULE', `El turno de ${label} del día ${body.dayNumber} ya existe.`, path)
    }
    const shift: MockShift = {
      id: newId(), studyId, dayNumber: body.dayNumber, type: body.type, status: 'ABIERTO',
      crewId: body.crewId ?? null, population: null, sourcesWithoutGeneration: [],
    }
    shiftsState.push(shift)
    return HttpResponse.json(toShiftResponse(shift), { status: 201 })
  }),

  http.get('/api/v1/shifts/:shiftId', ({ params }) => {
    const shift = findShift(params)
    return shift ? HttpResponse.json(toShiftResponse(shift)) : notFound(`/api/v1/shifts/${params.shiftId}`, 'Turno')
  }),

  http.post('/api/v1/shifts/:shiftId/close', ({ params }) => {
    const path = `/api/v1/shifts/${params.shiftId}/close`
    const denied = requirePermission('MANAGE_SHIFTS', path)
    if (denied) return denied
    const shift = findShift(params)
    if (!shift) return notFound(path, 'Turno')
    const pending = progressOf(shift).filter((source) => source.status === 'PENDIENTE').map((source) => source.code)
    if (pending.length > 0) {
      return apiError(422, 'ERR_BUSINESS_RULE', `No se puede cerrar el turno ${shiftCode(shift.dayNumber, shift.type)}: `
        + `faltan registros de las fuentes ${pending.sort().join(', ')}. Registre sus bolsas o márquelas como "sin generación".`,
      path)
    }
    shift.status = 'CERRADO'
    return HttpResponse.json(toShiftResponse(shift))
  }),

  http.get('/api/v1/shifts/:shiftId/population', ({ params }) => {
    const shift = findShift(params)
    if (!shift) return notFound(`/api/v1/shifts/${params.shiftId}/population`, 'Turno')
    return shift.population ? HttpResponse.json(shift.population) : new HttpResponse(null, { status: 204 })
  }),

  http.put('/api/v1/shifts/:shiftId/population', async ({ params, request }) => {
    const path = `/api/v1/shifts/${params.shiftId}/population`
    const shift = findShift(params)
    if (!shift) return notFound(path, 'Turno')
    const body = (await request.json()) as { students: number; teachers: number; staff: number }
    const total = Number(body.students) + Number(body.teachers) + Number(body.staff)
    if (total <= 0) {
      return apiError(400, 'ERR_INVALID_DATA', 'La población del turno debe ser mayor que cero: registre cuántas personas hay presentes.', path)
    }
    shift.population = {
      id: newId(), students: Number(body.students), teachers: Number(body.teachers), staff: Number(body.staff), total,
      verification: { status: 'PENDIENTE', enteredBy: activeUser?.id ?? null, enteredAt: new Date().toISOString(),
        verifiedBy: null, verifiedAt: null },
    }
    return HttpResponse.json(shift.population)
  }),

  // --- BAGS (section 5.2) ---
  http.get('/api/v1/shifts/:shiftId/bags', ({ params, request }) => {
    const bags = bagsState.filter((bag) => bag.shiftId === Number(params.shiftId))
    return HttpResponse.json(page(bags, new URL(request.url)))
  }),

  http.post('/api/v1/shifts/:shiftId/bags', async ({ params, request }) => {
    const path = `/api/v1/shifts/${params.shiftId}/bags`
    const shift = findShift(params)
    if (!shift) return notFound(path, 'Turno')
    if (!shift.population) {
      return apiError(422, 'ERR_BUSINESS_RULE', 'Registre primero la población del turno.', path)
    }
    const body = (await request.json()) as { sourceId: number; grossWeight: number; tare: number; volume: number }
    if (Number(body.tare) >= Number(body.grossWeight)) {
      const reason = 'La tara no puede ser mayor o igual que el peso bruto.'
      return apiError(400, 'ERR_INVALID_DATA', `Dato inválido en el campo 'tara': ${reason}`, path,
        [{ field: 'tare', message: reason }])
    }
    const source = sourcesState.find((item) => item.id === Number(body.sourceId))
    const code = source?.code ?? 'GEN'
    const sameSource = bagsState.filter((bag) => bag.shiftId === shift.id && bag.sourceId === Number(body.sourceId))
    const base = `${code}-${shiftCode(shift.dayNumber, shift.type)}`
    const netWeight = round2(Number(body.grossWeight) - Number(body.tare))
    const density = Number(body.volume) > 0 ? round2(netWeight / Number(body.volume)) : null
    const created: WasteBag = {
      id: newId(), shiftId: shift.id, sourceId: Number(body.sourceId), sourceCode: code,
      label: sameSource.length === 0 ? base : `${base}-${sameSource.length + 1}`,
      grossWeight: Number(body.grossWeight), tare: Number(body.tare), netWeight, volume: Number(body.volume), density,
      atypical: netWeight > 50 || (density !== null && (density < 20 || density > 1000)),
      verification: { status: 'PENDIENTE', enteredBy: activeUser?.id ?? null, enteredAt: new Date().toISOString(),
        verifiedBy: null, verifiedAt: null },
    }
    bagsState.push(created)
    if (created.atypical) {
      raiseAlert('VALOR_ATIPICO', 'Bolsa', created.id, `Valor atípico en la bolsa ${created.label}. Revise antes de verificar.`)
    }
    return HttpResponse.json(created, { status: 201 })
  }),

  http.post('/api/v1/shifts/:shiftId/sources/:sourceId/no-generation', ({ params }) => {
    const shift = findShift(params)
    if (!shift) return notFound(`/api/v1/shifts/${params.shiftId}`, 'Turno')
    const sourceId = Number(params.sourceId)
    if (!shift.sourcesWithoutGeneration.includes(sourceId)) {
      shift.sourcesWithoutGeneration.push(sourceId)
    }
    return HttpResponse.json(toShiftResponse(shift))
  }),

  http.delete('/api/v1/shifts/:shiftId/sources/:sourceId/no-generation', ({ params }) => {
    const shift = findShift(params)
    if (!shift) return notFound(`/api/v1/shifts/${params.shiftId}`, 'Turno')
    shift.sourcesWithoutGeneration = shift.sourcesWithoutGeneration.filter((id) => id !== Number(params.sourceId))
    return HttpResponse.json(toShiftResponse(shift))
  }),

  // --- QUARTERING AND DENSITY (section 5.3) ---
  http.get('/api/v1/shifts/:shiftId/quartering', ({ params }) => {
    const quartering = quarteringsState.find((item) => item.shiftId === Number(params.shiftId))
    return quartering ? HttpResponse.json(quartering)
      : notFound(`/api/v1/shifts/${params.shiftId}/quartering`, 'Cuarteo del turno')
  }),

  http.post('/api/v1/shifts/:shiftId/quartering', async ({ params, request }) => {
    const path = `/api/v1/shifts/${params.shiftId}/quartering`
    const shift = findShift(params)
    if (!shift) return notFound(path, 'Turno')
    if (quarteringsState.some((item) => item.shiftId === shift.id)) {
      return apiError(422, 'ERR_BUSINESS_RULE', `El turno ${shiftCode(shift.dayNumber, shift.type)} ya tiene un cuarteo registrado.`, path)
    }
    const body = (await request.json()) as { sampleWeight: number; results: { categoryId: number; weight: number }[] }
    const quartering = buildQuartering(shift, Number(body.sampleWeight), body.results)
    quarteringsState.push(quartering)
    if (quartering.qualityStatus === 'NO_CONFORME') {
      raiseAlert('CUARTEO_NO_CONFORME', 'Cuarteo', quartering.id, `Cuarteo no conforme en el turno ${shiftCode(shift.dayNumber, shift.type)}: la diferencia supera el 2 %. Vuelva a pesar.`)
    }
    return HttpResponse.json(quartering, { status: 201 })
  }),

  http.post('/api/v1/shifts/:shiftId/quartering/annul', async ({ params, request }) => {
    const path = `/api/v1/shifts/${params.shiftId}/quartering/annul`
    const quartering = quarteringsState.find((item) => item.shiftId === Number(params.shiftId))
    if (!quartering) return notFound(path, 'Cuarteo del turno')
    if (quartering.qualityStatus !== 'NO_CONFORME') {
      return apiError(422, 'ERR_BUSINESS_RULE', 'Solo se puede anular un cuarteo no conforme.', path)
    }
    const body = (await request.json()) as { reason?: string }
    if (!body.reason?.trim()) {
      return apiError(400, 'ERR_INVALID_DATA', 'Debe escribir el motivo para anular el cuarteo.', path,
        [{ field: 'reason', message: 'Debe escribir el motivo para anular el cuarteo.' }])
    }
    Object.assign(quartering, { qualityStatus: 'ANULADO', annulReason: body.reason.trim(),
      annulledBy: activeUser?.id ?? null, annulledAt: new Date().toISOString() })
    return HttpResponse.json(quartering)
  }),

  http.get('/api/v1/shifts/:shiftId/density', ({ params }) => {
    const density = densitiesState.find((item) => item.shiftId === Number(params.shiftId))
    return density ? HttpResponse.json(density)
      : notFound(`/api/v1/shifts/${params.shiftId}/density`, 'Medición de densidad del turno')
  }),

  http.post('/api/v1/shifts/:shiftId/density', async ({ params, request }) => {
    const path = `/api/v1/shifts/${params.shiftId}/density`
    const shift = findShift(params)
    if (!shift) return notFound(path, 'Turno')
    if (densitiesState.some((item) => item.shiftId === shift.id)) {
      return apiError(422, 'ERR_BUSINESS_RULE', `El turno ${shiftCode(shift.dayNumber, shift.type)} ya tiene una medición de densidad.`, path)
    }
    const body = (await request.json()) as { containerVolume: number; netWeight: number }
    const density: DensityMeasurement = {
      id: newId(), shiftId: shift.id, containerVolume: Number(body.containerVolume), netWeight: Number(body.netWeight),
      density: Number(body.containerVolume) > 0 ? round2(Number(body.netWeight) / Number(body.containerVolume)) : null,
      verification: pendingVerification(),
    }
    densitiesState.push(density)
    return HttpResponse.json(density, { status: 201 })
  }),

  // --- DOUBLE ENTRY, HISTORY AND ALERTS (spec 005) ---
  http.get('/api/v1/studies/:id/verification-summary', ({ params }) => {
    const shifts = shiftsState.filter((shift) => shift.studyId === Number(params.id)).map((shift) => {
      const records = recordsOf(shift)
      const count = (status: string) => records.filter((record) => record.verification.status === status).length
      const quartering = quarteringsState.find((item) => item.shiftId === shift.id)
      return { shiftId: shift.id, dayNumber: shift.dayNumber, type: shift.type, code: shiftCode(shift.dayNumber, shift.type),
        pending: count('PENDIENTE'), verified: count('VERIFICADO'), corrected: count('CORREGIDO'),
        nonConformingQuartering: quartering?.qualityStatus === 'NO_CONFORME' }
    })
    const sum = (key: 'pending' | 'verified' | 'corrected') => shifts.reduce((total, shift) => total + shift[key], 0)
    const nonConforming = shifts.filter((shift) => shift.nonConformingQuartering).length
    return HttpResponse.json({ shifts, pending: sum('pending'), verified: sum('verified'), corrected: sum('corrected'),
      nonConformingQuarterings: nonConforming, readyToClose: sum('pending') === 0 && nonConforming === 0 })
  }),

  http.get('/api/v1/shifts/:shiftId/records', ({ params }) => {
    const shift = findShift(params)
    return shift ? HttpResponse.json(recordsOf(shift)) : notFound(`/api/v1/shifts/${params.shiftId}/records`, 'Turno')
  }),

  http.get('/api/v1/records/:type/:id/history', ({ params }) => HttpResponse.json(
    correctionsState.filter((item) => item.type === params.type && item.id === Number(params.id)).map((item) => item.entry))),

  http.get('/api/v1/records/:type/:id', ({ params }) => {
    const record = findRecord(String(params.type), Number(params.id))
    return record ? HttpResponse.json(record) : notFound(`/api/v1/records/${params.type}/${params.id}`, 'Registro')
  }),

  http.post('/api/v1/verifications/:type/:id', ({ params }) => {
    const path = `/api/v1/verifications/${params.type}/${params.id}`
    const target = findTarget(String(params.type), Number(params.id))
    if (!target) return notFound(path, 'Registro')
    if (target.verification.enteredBy === activeUser?.id) {
      return apiError(422, 'ERR_BUSINESS_RULE', 'No puede verificar un registro que usted mismo digitó: debe hacerlo otra persona (doble registro).', path)
    }
    if (target.verification.status !== 'PENDIENTE') {
      return apiError(422, 'ERR_BUSINESS_RULE', 'El registro ya fue verificado.', path)
    }
    Object.assign(target.verification, { status: 'VERIFICADO', verifiedBy: activeUser?.id ?? null, verifiedAt: new Date().toISOString() })
    return HttpResponse.json(findRecord(String(params.type), Number(params.id)))
  }),

  http.post('/api/v1/verifications/:type/:id/correct', async ({ params, request }) => {
    const path = `/api/v1/verifications/${params.type}/${params.id}/correct`
    const type = String(params.type)
    const id = Number(params.id)
    const target = findTarget(type, id)
    if (!target) return notFound(path, 'Registro')
    if (target.verification.enteredBy === activeUser?.id) {
      return apiError(422, 'ERR_BUSINESS_RULE', 'No puede corregir un registro que usted mismo digitó: debe hacerlo otra persona (doble registro).', path)
    }
    const body = (await request.json()) as { values: Record<string, number>; reason?: string }
    if (!body.reason?.trim()) {
      return apiError(400, 'ERR_INVALID_DATA', 'El motivo de la corrección es obligatorio.', path,
        [{ field: 'reason', message: 'El motivo de la corrección es obligatorio.' }])
    }
    const changed = applyCorrection(type, id, body.values, body.reason.trim())
    if (changed === 0) {
      return apiError(400, 'ERR_INVALID_DATA', 'No hay cambios que corregir: los valores son iguales.', path)
    }
    Object.assign(target.verification, { status: 'CORREGIDO', verifiedBy: activeUser?.id ?? null, verifiedAt: new Date().toISOString() })
    return HttpResponse.json(findRecord(type, id))
  }),

  http.get('/api/v1/alerts', ({ request }) => {
    const url = new URL(request.url)
    const attended = url.searchParams.get('attended')
    const type = url.searchParams.get('type')
    const items = alertsState
      .filter((alert) => attended === null || String(alert.attended) === attended)
      .filter((alert) => !type || alert.type === type)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return HttpResponse.json(page(items, url))
  }),

  http.post('/api/v1/alerts/:id/attend', async ({ params, request }) => {
    const path = `/api/v1/alerts/${params.id}/attend`
    const alert = alertsState.find((item) => item.id === Number(params.id))
    if (!alert) return notFound(path, 'Alerta')
    if (alert.attended) return apiError(422, 'ERR_BUSINESS_RULE', 'La alerta ya fue atendida.', path)
    const body = (await request.json()) as { reason?: string }
    if (!body.reason?.trim()) {
      return apiError(400, 'ERR_INVALID_DATA', 'Escriba una observación sobre cómo se atendió la alerta.', path,
        [{ field: 'reason', message: 'Escriba una observación sobre cómo se atendió la alerta.' }])
    }
    Object.assign(alert, { attended: true, attendedBy: activeUser?.id ?? null, attendedAt: new Date().toISOString(),
      note: body.reason.trim() })
    return HttpResponse.json(alert)
  }),
]

type MockVerification = WasteBag['verification']

function field(name: string, label: string, value: number | string | null, editable: boolean): RecordField {
  return { field: name, label, value, editable }
}

function recordsOf(shift: MockShift): VerifiableRecord[] {
  const code = shiftCode(shift.dayNumber, shift.type)
  const base = { shiftId: shift.id, shiftCode: code }
  const records: VerifiableRecord[] = []
  if (shift.population) {
    const population = shift.population
    records.push({ ...base, recordType: 'population', recordName: 'Población del turno', recordId: population.id ?? 0,
      title: `Población del turno ${code}`, verification: population.verification as MockVerification,
      fields: [field('students', 'Estudiantes', population.students, true), field('teachers', 'Docentes', population.teachers, true),
        field('staff', 'Personal', population.staff, true), field('total', 'Total', population.total, false)] })
  }
  bagsState.filter((bag) => bag.shiftId === shift.id).forEach((bag) => records.push({ ...base, recordType: 'bag',
    recordName: 'Bolsa', recordId: bag.id, title: `Bolsa ${bag.label}`, verification: bag.verification,
    fields: [field('source', 'Fuente', bag.sourceCode, false), field('grossWeight', 'Peso bruto (kg)', bag.grossWeight, true),
      field('tare', 'Tara (kg)', bag.tare, true), field('volume', 'Volumen (m³)', bag.volume, true),
      field('netWeight', 'Peso neto (kg)', bag.netWeight, false), field('density', 'Densidad (kg/m³)', bag.density, false)] }))
  const quartering = quarteringsState.find((item) => item.shiftId === shift.id)
  if (quartering) {
    records.push({ ...base, recordType: 'quartering', recordName: 'Cuarteo', recordId: quartering.id, title: `Cuarteo ${code}`,
      verification: quartering.verification,
      fields: [field('sampleWeight', 'Submuestra (kg)', quartering.sampleWeight, true),
        ...quartering.results.map((result) => field(`result:${result.categoryId}`, `${result.categoryName} (kg)`, result.weight, true)),
        field('differencePercent', 'Diferencia (%)', quartering.differencePercent, false),
        field('qualityStatus', 'Control del 2 %', quartering.qualityStatus === 'CONFORME' ? 'Conforme'
          : quartering.qualityStatus === 'NO_CONFORME' ? 'No conforme' : 'Anulado', false)] })
  }
  const density = densitiesState.find((item) => item.shiftId === shift.id)
  if (density) {
    records.push({ ...base, recordType: 'density', recordName: 'Medición de densidad', recordId: density.id,
      title: `Medición de densidad ${code}`, verification: density.verification,
      fields: [field('containerVolume', 'Volumen del recipiente (m³)', density.containerVolume, true),
        field('netWeight', 'Peso neto (kg)', density.netWeight, true), field('density', 'Densidad (kg/m³)', density.density, false)] })
  }
  return records
}

function findRecord(type: string, id: number): VerifiableRecord | undefined {
  return shiftsState.flatMap(recordsOf).find((record) => record.recordType === type && record.recordId === id)
}

function findTarget(type: string, id: number): { verification: MockVerification } | undefined {
  if (type === 'population') {
    const population = shiftsState.find((shift) => shift.population?.id === id)?.population
    return population ? { verification: population.verification as MockVerification } : undefined
  }
  if (type === 'bag') return bagsState.find((bag) => bag.id === id)
  if (type === 'quartering') return quarteringsState.find((item) => item.id === id)
  if (type === 'density') return densitiesState.find((item) => item.id === id)
  return undefined
}

function applyCorrection(type: string, id: number, values: Record<string, number>, reason: string): number {
  const record = findRecord(type, id)
  let changed = 0
  for (const [name, value] of Object.entries(values ?? {})) {
    const current = record?.fields.find((item) => item.field === name)
    if (!current || !current.editable || Number(current.value) === Number(value)) continue
    changed += 1
    correctionsState.push({ type, id, entry: { field: name, oldValue: String(current.value), newValue: String(value), reason,
      userId: activeUser?.id ?? 0, at: new Date().toISOString() } })
    if (type === 'population') {
      const population = shiftsState.find((shift) => shift.population?.id === id)?.population as unknown as Record<string, number>
      population[name] = Number(value)
      population.total = population.students + population.teachers + population.staff
    } else if (type === 'bag') {
      const bag = bagsState.find((item) => item.id === id) as WasteBag
      ;(bag as unknown as Record<string, number>)[name] = Number(value)
      bag.netWeight = round2(bag.grossWeight - bag.tare)
      bag.density = bag.volume > 0 ? round2(bag.netWeight / bag.volume) : null
    } else if (type === 'density') {
      const density = densitiesState.find((item) => item.id === id) as DensityMeasurement
      ;(density as unknown as Record<string, number>)[name] = Number(value)
      density.density = density.containerVolume > 0 ? round2(density.netWeight / density.containerVolume) : null
    } else if (type === 'quartering') {
      const quartering = quarteringsState.find((item) => item.id === id) as Quartering
      if (name === 'sampleWeight') quartering.sampleWeight = Number(value)
      const result = quartering.results.find((item) => `result:${item.categoryId}` === name)
      if (result) result.weight = Number(value)
    }
  }
  if (type === 'quartering' && changed > 0) {
    const quartering = quarteringsState.find((item) => item.id === id) as Quartering
    const shift = shiftsState.find((item) => item.id === quartering.shiftId) as MockShift
    const rebuilt = buildQuartering(shift, quartering.sampleWeight,
      quartering.results.map((item) => ({ categoryId: item.categoryId, weight: item.weight })))
    Object.assign(quartering, { ...rebuilt, id: quartering.id, verification: quartering.verification })
  }
  return changed
}

function raiseAlert(type: Alert['type'], targetType: string, targetId: number, description: string) {
  alertsState.push({ id: newId(), type, targetType, targetId, description, createdAt: new Date().toISOString(),
    attended: false, attendedBy: null, attendedAt: null, note: null })
}

function pendingVerification() {
  return { status: 'PENDIENTE' as const, enteredBy: activeUser?.id ?? null, enteredAt: new Date().toISOString(),
    verifiedBy: null, verifiedAt: null }
}

function buildQuartering(shift: MockShift, sampleWeight: number,
                         inputs: { categoryId: number; weight: number }[]): Quartering {
  const results = inputs.map((input) => {
    const category = categoriesState.find((item) => item.id === input.categoryId)
    return { categoryId: input.categoryId, categoryName: category?.name ?? `#${input.categoryId}`,
      group: (category?.group ?? 'NEGRO') as WasteGroup, weight: Number(input.weight), percentage: null as number | null }
  })
  const total = round2(results.reduce((sum, item) => sum + item.weight, 0))
  results.forEach((item) => { item.percentage = total > 0 ? round2((item.weight * 100) / total) : 0 })
  const groupComposition = { BLANCO: 0, VERDE: 0, NEGRO: 0, SIN_COLOR: 0 } as Record<WasteGroup, number>
  for (const group of Object.keys(groupComposition) as WasteGroup[]) {
    const weight = results.filter((item) => item.group === group).reduce((sum, item) => sum + item.weight, 0)
    groupComposition[group] = total > 0 ? round2((weight * 100) / total) : 0
  }
  const differenceKg = round2(Math.abs(sampleWeight - total))
  const differencePercent = round2((differenceKg * 100) / sampleWeight)
  const shiftNet = round2(bagsState.filter((bag) => bag.shiftId === shift.id).reduce((sum, bag) => sum + bag.netWeight, 0))
  return {
    id: newId(), shiftId: shift.id, shiftNetWeight: shiftNet, sampleWeight, totalClassifiedWeight: total,
    differenceKg, differencePercent, qualityStatus: differenceKg * 100 <= sampleWeight * 2 + 1e-9 ? 'CONFORME' : 'NO_CONFORME',
    results, groupComposition, annulReason: null, annulledBy: null, annulledAt: null, verification: pendingVerification(),
  }
}

function cloneShifts(): MockShift[] {
  return structuredClone(mockShifts)
}

/** Resets the mock state between tests; null leaves the user logged out. */
export function resetMockState(userRole?: UserRole | null) {
  activeUser = userRole === null
    ? null
    : mockUsers.find((user) => user.role === (userRole || 'DOCENTE')) || mockUsers[0]
  institutionsState = [{ ...mockInstitution }]
  usersState = [...mockUsers]
  sourcesState = mockSources.map((source) => ({ ...source }))
  crewsState = [...mockCrews]
  categoriesState = [...mockCategories]
  studiesState = [{ ...mockStudy, days: mockStudy.days.map((day) => ({ ...day })),
    participatingSourceIds: [...mockStudy.participatingSourceIds] }]
  shiftsState = cloneShifts()
  bagsState = structuredClone(mockBags)
  quarteringsState = []
  densitiesState = []
  alertsState = []
  correctionsState = []
  nextId = 1000
}

/** Changes the user of the mock session. */
export function setActiveUser(user: User | null) {
  activeUser = user
}
