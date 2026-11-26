/**
 * Step-by-step guide that Rokko shows on each screen, so the user does not
 * need the manual (professor's review, point 3).
 *
 * @author craulgomez
 */

export interface Tip {
  title: string
  steps: string[]
}

export const TIPS = {
  institutions: {
    title: 'Registrar una institución',
    steps: [
      'Pulsa «Nueva Institución».',
      'Escribe el nombre del plantel, por ejemplo «I.E. Prudencia Daza».',
      'Arma la dirección: tipo de vía, números y, si quieres, el barrio. Abajo verás cómo queda.',
      'Pulsa «Guardar Institución».',
    ],
  },
  sources: {
    title: 'Registrar una fuente de residuos',
    steps: [
      'Escoge la institución en la lista de arriba.',
      'Pulsa «Nueva Fuente».',
      'Escribe el nombre del espacio y escoge su tipo. El código se crea solo.',
      'Pulsa «Guardar Fuente».',
    ],
  },
  categories: {
    title: 'Registrar un tipo de residuo',
    steps: [
      'Pulsa «Nueva Subcategoría».',
      'Escribe el nombre, por ejemplo «Botellas PET».',
      'Escoge el grupo de color: blanco (aprovechables), verde (orgánicos), negro (no aprovechables) o sin color (peligrosos).',
      'Pulsa «Guardar Subcategoría».',
    ],
  },
  users: {
    title: 'Registrar un docente o un estudiante',
    steps: [
      'Pulsa «Nuevo Usuario».',
      'Escribe nombre, apellido y el correo institucional (con @).',
      'Escoge el rol: Docente o Estudiante universitario.',
      'Pon una contraseña inicial de 8 caracteres o más y compártela con la persona.',
      'Pulsa «Guardar Usuario».',
    ],
  },
  crews: {
    title: 'Armar una cuadrilla',
    steps: [
      'Pulsa «Nueva Cuadrilla» y escribe su nombre.',
      'Escoge el docente responsable.',
      'Marca los estudiantes que van a pesar en campo.',
      'Pulsa «Guardar Cuadrilla». Luego la asignas a un estudio.',
    ],
  },
  studies: {
    title: 'Crear un estudio de caracterización',
    steps: [
      'Pulsa «Nuevo estudio».',
      'Escoge la institución y la cuadrilla.',
      'Elige la fecha del día 1 (desde hoy). Los otros 7 días se calculan solos, sin fines de semana ni festivos.',
      'Revisa las fuentes que participan y pulsa «Crear estudio».',
      'En el detalle del estudio pulsa «Iniciar ejecución» para empezar a registrar en campo.',
    ],
  },
  field: {
    title: 'Empezar un turno',
    steps: [
      'Escoge el día y el turno (te sugiero el de hoy).',
      'Pulsa «Abrir turno» y luego «Confirmar apertura».',
      'Dentro del turno registra la población y después las bolsas.',
    ],
  },
  shift: {
    title: 'Completar el turno',
    steps: [
      'Registra la población: cuántas personas hay hoy.',
      'Pesa las bolsas de cada fuente con «Registrar bolsa».',
      'Si una fuente no generó residuos, pulsa «Marcar sin generación».',
      'Haz el cuarteo y la densidad desde los pasos de arriba.',
      'Cuando no queden fuentes pendientes, pulsa «Cerrar turno».',
    ],
  },
  population: {
    title: 'Registrar la población',
    steps: [
      'Cuenta los estudiantes, docentes y personal presentes en el turno.',
      'Usa los botones + y − o escribe el número.',
      'Pulsa «Guardar población». Sin ella no se pueden pesar bolsas.',
    ],
  },
  bag: {
    title: 'Registrar una bolsa',
    steps: [
      'Toca la fuente de donde viene la bolsa.',
      'Escribe el peso bruto y la tara del recipiente, en kg.',
      'Escribe el volumen de la bolsa en m³.',
      'Revisa el peso neto y la densidad que calculo por ti.',
      'Pulsa «Guardar bolsa» y escribe en la bolsa el rótulo que aparece.',
    ],
  },
  quartering: {
    title: 'Hacer el cuarteo',
    steps: [
      'Escribe el peso de la submuestra (te propongo uno si el turno pesó poco).',
      'Separa los residuos y escribe el peso de cada subcategoría.',
      'Mira la barra: la diferencia debe quedar en 2 % o menos.',
      'Pulsa «Guardar cuarteo».',
    ],
  },
  density: {
    title: 'Medir la densidad',
    steps: [
      'Escoge el recipiente o escribe su volumen en m³.',
      'Escribe el peso neto de los residuos que caben en él.',
      'Pulsa «Guardar densidad».',
    ],
  },
  verification: {
    title: 'Verificar los registros',
    steps: [
      'Escoge el estudio y el turno.',
      'Compara cada registro con la planilla en papel.',
      'Si coincide, pulsa «Coincide con el papel». Si no, pulsa «Corregir» y explica el motivo.',
    ],
  },
} satisfies Record<string, Tip>

export type TipId = keyof typeof TIPS
