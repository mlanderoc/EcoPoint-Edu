import { http, HttpResponse } from 'msw'
import {
  mockInstitution,
  mockUsers,
  mockSources,
  mockCrews,
  mockCategories,
  mockStudy,
  mockShifts,
  mockBags,
} from './data/seedData'
import { CharacterizationStudy, GenerationSource, SamplingShift, User, WasteBag } from '@/types'

// In-memory mutable state for the mock session
let activeUser: User | null = mockUsers[0] // Default logged in as Docente for development convenience
let usersState: User[] = [...mockUsers]
let sourcesState: GenerationSource[] = [...mockSources]
let studyState: CharacterizationStudy = { ...mockStudy }
let shiftsState: SamplingShift[] = [...mockShifts]
let bagsState: WasteBag[] = [...mockBags]

export const handlers = [
  // --- AUTHENTICATION ---
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { email?: string; password?: string }
    const { email, password } = body

    // Match credentials
    const found = usersState.find(
      (u) =>
        u.email.toLowerCase() === email?.toLowerCase() &&
        (password === 'Docente123*' || password === 'Estudiante123*')
    )

    if (!found) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 401,
          errorCode: 'ERR_UNAUTHORIZED',
          message: 'Correo electrónico o contraseña incorrectos.',
          path: '/api/v1/auth/login',
          fieldErrors: [],
        },
        { status: 401 }
      )
    }

    activeUser = found
    return HttpResponse.json(found, { status: 200 })
  }),

  http.post('/api/v1/auth/logout', () => {
    activeUser = null
    return HttpResponse.json({ success: true }, { status: 200 })
  }),

  http.get('/api/v1/auth/me', () => {
    if (!activeUser) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 401,
          errorCode: 'ERR_UNAUTHORIZED',
          message: 'Su sesión expiró o no ha iniciado sesión.',
          path: '/api/v1/auth/me',
          fieldErrors: [],
        },
        { status: 401 }
      )
    }
    return HttpResponse.json(activeUser, { status: 200 })
  }),

  // --- INSTITUTIONS & SOURCES ---
  http.get('/api/v1/institutions', () => {
    return HttpResponse.json([mockInstitution], { status: 200 })
  }),

  http.get('/api/v1/institutions/:id/sources', () => {
    return HttpResponse.json(sourcesState, { status: 200 })
  }),

  http.post('/api/v1/institutions/:id/sources', async ({ request }) => {
    const body = (await request.json()) as Omit<GenerationSource, 'id'>
    const newSource: GenerationSource = {
      ...body,
      id: Date.now(),
      institutionId: 1,
      active: true,
    }
    sourcesState.push(newSource)
    return HttpResponse.json(newSource, { status: 201 })
  }),

  // --- USERS & CREWS ---
  http.get('/api/v1/users', () => {
    return HttpResponse.json(usersState, { status: 200 })
  }),

  http.post('/api/v1/users', async ({ request }) => {
    const body = (await request.json()) as Omit<User, 'id'>
    // Validation: duplicate email (CA-302)
    const exists = usersState.some(
      (u) => u.email.toLowerCase() === body.email.toLowerCase()
    )
    if (exists) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 400,
          errorCode: 'ERR_INVALID_DATA',
          message: 'Los datos enviados contienen errores.',
          path: '/api/v1/users',
          fieldErrors: [
            {
              field: 'email',
              message: 'El correo electrónico ya se encuentra registrado.',
            },
          ],
        },
        { status: 400 }
      )
    }

    const newUser: User = {
      ...body,
      id: Date.now(),
      active: true,
    }
    usersState.push(newUser)
    return HttpResponse.json(newUser, { status: 201 })
  }),

  http.get('/api/v1/crews', () => {
    return HttpResponse.json(mockCrews, { status: 200 })
  }),

  http.get('/api/v1/waste-categories', () => {
    return HttpResponse.json(mockCategories, { status: 200 })
  }),

  // --- CHARACTERIZATION STUDY ---
  http.get('/api/v1/studies', () => {
    return HttpResponse.json([studyState], { status: 200 })
  }),

  http.get('/api/v1/studies/:id', () => {
    return HttpResponse.json(studyState, { status: 200 })
  }),

  http.post('/api/v1/studies', async ({ request }) => {
    const body = (await request.json()) as Partial<CharacterizationStudy>
    studyState = {
      ...mockStudy,
      ...body,
      id: Date.now(),
      status: 'PLANIFICADO',
    } as CharacterizationStudy
    return HttpResponse.json(studyState, { status: 201 })
  }),

  http.post('/api/v1/studies/:id/close', () => {
    // CA-309: Motivo exacto visible si quedan registros pendientes
    return HttpResponse.json(
      {
        timestamp: new Date().toISOString(),
        status: 422,
        errorCode: 'ERR_BUSINESS_RULE',
        message: 'No se puede cerrar el estudio: Quedan 4 registros pendientes de verificación.',
        path: '/api/v1/studies/1/close',
        fieldErrors: [],
      },
      { status: 422 }
    )
  }),

  // --- SHIFTS & FIELD REGISTRATION ---
  http.get('/api/v1/studies/:id/shifts', () => {
    return HttpResponse.json(shiftsState, { status: 200 })
  }),

  http.get('/api/v1/shifts/:id', ({ params }) => {
    const shift = shiftsState.find((s) => s.id === Number(params.id))
    if (!shift) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 404,
          errorCode: 'ERR_NOT_FOUND',
          message: 'Turno de muestreo no encontrado.',
          path: `/api/v1/shifts/${params.id}`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(shift, { status: 200 })
  }),

  http.post('/api/v1/shifts/:id/open', ({ params }) => {
    const shift = shiftsState.find((s) => s.id === Number(params.id))
    if (shift) {
      shift.status = 'ABIERTO'
      return HttpResponse.json(shift, { status: 200 })
    }
    return HttpResponse.json({ status: 404 }, { status: 404 })
  }),

  // Population registration (R-342)
  http.get('/api/v1/shifts/:id/population', ({ params }) => {
    const shift = shiftsState.find((s) => s.id === Number(params.id))
    return HttpResponse.json(shift?.population ?? null, { status: 200 })
  }),

  http.post('/api/v1/shifts/:id/population', async ({ params, request }) => {
    const shift = shiftsState.find((s) => s.id === Number(params.id))
    if (!shift) return HttpResponse.json({ status: 404 }, { status: 404 })

    const body = (await request.json()) as {
      students: number
      teachers: number
      staff: number
    }

    const total = Number(body.students || 0) + Number(body.teachers || 0) + Number(body.staff || 0)
    shift.population = {
      students: Number(body.students || 0),
      teachers: Number(body.teachers || 0),
      staff: Number(body.staff || 0),
      total,
    }
    return HttpResponse.json(shift.population, { status: 200 })
  }),

  // Bags (R-343, CA-303, CA-304, CA-305, CA-310)
  http.get('/api/v1/shifts/:id/bags', ({ params }) => {
    const filtered = bagsState.filter((b) => b.shiftId === Number(params.id))
    return HttpResponse.json(filtered, { status: 200 })
  }),

  http.post('/api/v1/shifts/:id/bags', async ({ params, request }) => {
    const shiftId = Number(params.id)
    const shift = shiftsState.find((s) => s.id === shiftId)
    if (!shift) return HttpResponse.json({ status: 404 }, { status: 404 })

    // CA-303 / RN-023: No bags before population
    if (!shift.population || shift.population.total <= 0) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 422,
          errorCode: 'ERR_BUSINESS_RULE',
          message: 'No se pueden registrar bolsas: registre primero la población del turno.',
          path: `/api/v1/shifts/${shiftId}/bags`,
          fieldErrors: [],
        },
        { status: 422 }
      )
    }

    const body = (await request.json()) as {
      sourceId: number
      grossWeight: number
      tare: number
      volume: number
    }

    // CA-305: tare >= grossWeight validation
    if (Number(body.tare) >= Number(body.grossWeight)) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 400,
          errorCode: 'ERR_INVALID_DATA',
          message: 'Los datos enviados contienen errores.',
          path: `/api/v1/shifts/${shiftId}/bags`,
          fieldErrors: [
            {
              field: 'tare',
              message: 'La tara no puede superar el peso bruto.',
            },
          ],
        },
        { status: 400 }
      )
    }

    const source = sourcesState.find((s) => s.id === Number(body.sourceId))
    const sourceCode = source ? source.code : 'GEN'
    const shiftLetter = shift.type === 'MANANA' ? 'M' : 'T'
    const label = `${sourceCode}-D${shift.dayNumber}-${shiftLetter}`

    const gross = Number(body.grossWeight)
    const tare = Number(body.tare)
    const vol = Number(body.volume)
    const netWeight = Number((gross - tare).toFixed(2))
    const density = vol > 0 ? Number((netWeight / vol).toFixed(2)) : 0

    const newBag: WasteBag = {
      id: Date.now(),
      shiftId,
      sourceId: Number(body.sourceId),
      sourceCode,
      label,
      grossWeight: gross,
      tare,
      netWeight,
      volume: vol,
      density,
      verificationStatus: 'PENDIENTE',
    }

    bagsState.push(newBag)
    return HttpResponse.json(newBag, { status: 201 })
  }),

  // Mark source without generation (R-345)
  http.post(
    '/api/v1/shifts/:id/sources/:sourceId/no-generation',
    ({ params }) => {
      const shift = shiftsState.find((s) => s.id === Number(params.id))
      const sourceId = Number(params.sourceId)
      if (shift && !shift.sourcesWithoutGeneration.includes(sourceId)) {
        shift.sourcesWithoutGeneration.push(sourceId)
      }
      return HttpResponse.json({ success: true }, { status: 200 })
    }
  ),

  // Close shift (R-346, CA-306)
  http.post('/api/v1/shifts/:id/close', ({ params }) => {
    const shift = shiftsState.find((s) => s.id === Number(params.id))
    if (!shift) return HttpResponse.json({ status: 404 }, { status: 404 })

    // Check if any source is pending
    const shiftBags = bagsState.filter((b) => b.shiftId === shift.id)
    const sourcesWithBags = new Set(shiftBags.map((b) => b.sourceId))
    const sourcesWithoutGen = new Set(shift.sourcesWithoutGeneration)

    const pending = studyState.participatingSourceIds.filter(
      (sId) => !sourcesWithBags.has(sId) && !sourcesWithoutGen.has(sId)
    )

    if (pending.length > 0) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 422,
          errorCode: 'ERR_BUSINESS_RULE',
          message: 'No se puede cerrar el turno: existen fuentes pendientes de registro.',
          path: `/api/v1/shifts/${shift.id}/close`,
          fieldErrors: [],
        },
        { status: 422 }
      )
    }

    shift.status = 'CERRADO'
    return HttpResponse.json(shift, { status: 200 })
  }),
]

// Reset helper for tests
export function resetMockState(userRole: 'DOCENTE' | 'ESTUDIANTE_UNIVERSITARIO' = 'DOCENTE') {
  activeUser = mockUsers.find((u) => u.role === userRole) || mockUsers[0]
  usersState = [...mockUsers]
  sourcesState = [...mockSources]
  studyState = { ...mockStudy }
  shiftsState = [
    {
      ...mockShifts[0],
      population: { students: 450, teachers: 18, staff: 8, total: 476 },
      sourcesWithoutGeneration: [],
      status: 'ABIERTO',
    },
  ]
  bagsState = [...mockBags]
}
