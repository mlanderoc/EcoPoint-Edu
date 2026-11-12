import {
  CharacterizationStudy,
  Crew,
  GenerationSource,
  Institution,
  SamplingShift,
  User,
  WasteBag,
  WasteCategory,
} from '@/types'

export const mockInstitution: Institution = {
  id: 1,
  name: 'I.E. Prudencia Daza',
  address: 'Calle 16 # 19-32, Valledupar, Cesar',
  mapCenter: {
    latitude: 10.4742,
    longitude: -73.2436,
  },
  mapZoom: 18,
}

export const mockUsers: User[] = [
  {
    id: 1,
    firstName: 'Docente',
    lastName: 'Demo',
    email: 'docente.demo@unicesar.edu.co',
    role: 'DOCENTE',
    active: true,
    institutionId: 1,
  },
  {
    id: 2,
    firstName: 'Estudiante',
    lastName: 'Demo',
    email: 'estudiante.demo@unicesar.edu.co',
    role: 'ESTUDIANTE_UNIVERSITARIO',
    active: true,
    institutionId: 1,
  },
  {
    id: 3,
    firstName: 'Laura',
    lastName: 'Ríos',
    email: 'laura.rios@unicesar.edu.co',
    role: 'ESTUDIANTE_UNIVERSITARIO',
    active: true,
    institutionId: 1,
  },
  {
    id: 4,
    firstName: 'Andrés',
    lastName: 'Mendoza',
    email: 'andres.mendoza@unicesar.edu.co',
    role: 'ESTUDIANTE_UNIVERSITARIO',
    active: true,
    institutionId: 1,
  },
  {
    id: 5,
    firstName: 'Valentina',
    lastName: 'Arias',
    email: 'valentina.arias@unicesar.edu.co',
    role: 'ESTUDIANTE_UNIVERSITARIO',
    active: true,
    institutionId: 1,
  },
]

export const mockSources: GenerationSource[] = [
  { id: 1, institutionId: 1, code: 'AU', name: 'Aulas de Clase', type: 'AULAS', active: true },
  { id: 2, institutionId: 1, code: 'BA', name: 'Baterías Sanitarias', type: 'BANOS', active: true },
  { id: 3, institutionId: 1, code: 'TE', name: 'Tienda Escolar', type: 'TIENDA_ESCOLAR', active: true },
  { id: 4, institutionId: 1, code: 'OF', name: 'Oficinas Administrativas', type: 'OFICINAS', active: true },
  { id: 5, institutionId: 1, code: 'PAE', name: 'Programa de Alimentación Escolar (PAE)', type: 'PAE', active: true },
  { id: 6, institutionId: 1, code: 'ZV', name: 'Patios y Zonas Verdes', type: 'ZONAS_VERDES', active: true },
]

export const mockCrews: Crew[] = [
  { id: 1, name: 'Cuadrilla Mañana A', responsibleId: 1, memberIds: [2] },
  { id: 2, name: 'Cuadrilla Tarde B', responsibleId: 1, memberIds: [2] },
]

export const mockCategories: WasteCategory[] = [
  { id: 1, name: 'Papel de archivo', group: 'BLANCO', active: true },
  { id: 2, name: 'Cartón', group: 'BLANCO', active: true },
  { id: 3, name: 'Plástico PET', group: 'BLANCO', active: true },
  { id: 4, name: 'Restos de comida', group: 'VERDE', active: true },
  { id: 5, name: 'Hojarasca y podas', group: 'VERDE', active: true },
  { id: 6, name: 'Papel higiénico y servilletas', group: 'NEGRO', active: true },
  { id: 7, name: 'Envolturas metalizadas', group: 'NEGRO', active: true },
  { id: 8, name: 'Pilas y baterías', group: 'SIN_COLOR', active: true },
]

export const mockStudy: CharacterizationStudy = {
  id: 1,
  institutionId: 1,
  name: 'Línea Base Prudencia Daza 2026',
  status: 'EN_EJECUCION',
  startDate: '2026-03-02',
  endDate: '2026-03-11',
  participatingSourceIds: [1, 2, 3, 4, 5, 6],
  days: [
    { dayNumber: 1, date: '2026-03-02', excluded: true },
    { dayNumber: 2, date: '2026-03-03', excluded: false },
    { dayNumber: 3, date: '2026-03-04', excluded: false },
    { dayNumber: 4, date: '2026-03-05', excluded: false },
    { dayNumber: 5, date: '2026-03-06', excluded: false },
    { dayNumber: 6, date: '2026-03-09', excluded: false },
    { dayNumber: 7, date: '2026-03-10', excluded: false },
    { dayNumber: 8, date: '2026-03-11', excluded: false },
  ],
}

export const mockShifts: SamplingShift[] = [
  {
    id: 14,
    studyId: 1,
    dayNumber: 3,
    type: 'MANANA',
    status: 'ABIERTO',
    crewId: 1,
    population: {
      students: 450,
      teachers: 18,
      staff: 8,
      total: 476,
    },
    sourcesWithoutGeneration: [],
  },
]

export const mockBags: WasteBag[] = [
  {
    id: 101,
    shiftId: 14,
    sourceId: 4,
    sourceCode: 'OF',
    label: 'OF-D3-M',
    grossWeight: 4.5,
    tare: 0.3,
    netWeight: 4.2,
    volume: 0.035,
    density: 120.0,
    verificationStatus: 'PENDIENTE',
  },
]
