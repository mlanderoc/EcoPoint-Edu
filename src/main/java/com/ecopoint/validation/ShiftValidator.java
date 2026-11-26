package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftPopulation;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stateless validator for sampling shifts and their population (RN-020 to
 * RN-025), applying GRASP Pure Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class ShiftValidator {

    private static final int MAX_STUDENTS = 5000;
    private static final int MAX_TEACHERS = 500;
    private static final int MAX_STAFF = 100;

    private ShiftValidator() {
    }

    /**
     * Validates that a new shift may be opened: the study is being executed and
     * there is no other shift of the same day and type (RN-020).
     *
     * @param study          study the shift belongs to
     * @param dayNumber      sampling day, 1 to 8
     * @param type           morning or afternoon
     * @param existingShifts shifts already opened in the study
     * @throws BusinessRuleException if the study is not executing, the shift already exists,
     *                               another shift is still open or the previous shift is not closed
     */
    public static void validateNewShift(CharacterizationStudy study, int dayNumber, ShiftType type,
                                        List<SamplingShift> existingShifts) {
        requireStudyInExecution(study);
        if (existingShifts == null) {
            return;
        }
        boolean duplicated = existingShifts.stream()
                .anyMatch(shift -> shift.getDayNumber() == dayNumber && shift.getType() == type);
        if (duplicated) {
            throw new BusinessRuleException("El turno de " + type.getDisplayName().toLowerCase()
                    + " del día " + dayNumber + " ya existe.");
        }
        existingShifts.stream().filter(SamplingShift::isOpen).findFirst().ifPresent(open -> {
            throw new BusinessRuleException("Cierre el turno " + open.getCode() + " antes de abrir otro.");
        });
        requirePreviousShiftClosed(dayNumber, type, existingShifts);
    }

    /**
     * The shifts are worked in order: day 1 morning, day 1 afternoon, day 2
     * morning... so a shift opens only when the one before it is closed.
     */
    private static void requirePreviousShiftClosed(int dayNumber, ShiftType type, List<SamplingShift> existingShifts) {
        if (dayNumber == 1 && type == ShiftType.MANANA) {
            return;
        }
        int previousDay = type == ShiftType.TARDE ? dayNumber : dayNumber - 1;
        ShiftType previousType = type == ShiftType.TARDE ? ShiftType.MANANA : ShiftType.TARDE;
        boolean previousClosed = existingShifts.stream()
                .anyMatch(shift -> shift.getDayNumber() == previousDay && shift.getType() == previousType
                        && !shift.isOpen());
        if (!previousClosed) {
            throw new BusinessRuleException("Los turnos van en orden: primero se cierra el turno de la "
                    + previousType.getDisplayName().toLowerCase() + " del día " + previousDay + ".");
        }
    }

    /**
     * Validates the population of a shift: every count lies in its logical
     * range (up to 5000 students, 500 teachers and 100 staff) and the total is
     * greater than zero (RN-022; professor's review, point 1).
     *
     * @param population population to validate
     * @throws InvalidDataException if a count is out of range or nobody is present
     */
    public static void validatePopulation(ShiftPopulation population) {
        if (population == null) {
            throw new InvalidDataException("poblacion", "La población del turno no puede ser nula.");
        }
        ValidationSupport.requireCount(population.getStudents(), MAX_STUDENTS, "estudiantes",
                "La cantidad de estudiantes");
        ValidationSupport.requireCount(population.getTeachers(), MAX_TEACHERS, "docentes",
                "La cantidad de docentes");
        ValidationSupport.requireCount(population.getStaff(), MAX_STAFF, "personal", "La cantidad de personal");
        if (population.getTotal() <= 0) {
            throw new InvalidDataException("poblacion",
                    "La población del turno debe ser mayor que cero: registre cuántas personas hay presentes.");
        }
    }

    /**
     * Validates that the shift may receive field data now: the shift is open
     * and the study is being executed (RN-014, RN-021).
     *
     * @param shift shift that will receive the data
     * @param study study the shift belongs to
     * @throws BusinessRuleException if the shift is closed or the study is not executing
     */
    public static void validateOpenForRegistration(SamplingShift shift, CharacterizationStudy study) {
        ValidationSupport.requireNonNull(shift, "turno", "El turno no puede ser nulo.");
        requireStudyInExecution(study);
        if (!shift.isOpen()) {
            throw new BusinessRuleException("El turno " + shift.getCode()
                    + " está cerrado: no se pueden registrar datos.");
        }
    }

    /**
     * Validates that bags may be registered in the shift: besides being open,
     * the shift must already have its population (RN-023).
     *
     * @param shift shift that will receive the bag
     * @param study study the shift belongs to
     * @throws BusinessRuleException if the shift is closed, the study is not executing
     *                               or the population is missing
     */
    public static void validateCanRegisterBag(SamplingShift shift, CharacterizationStudy study) {
        validateOpenForRegistration(shift, study);
        if (!shift.hasPopulation()) {
            throw new BusinessRuleException("Registre primero la población del turno.");
        }
    }

    /**
     * Validates that the study still admits changes in its shifts and
     * quarterings: not closed nor cancelled (RN-100).
     *
     * @param study study of the shift
     * @throws BusinessRuleException if the study is finished
     */
    public static void validateStudyNotFinished(CharacterizationStudy study) {
        ValidationSupport.requireNonNull(study, "estudio", "El estudio no puede ser nulo.");
        if (study.getStatus().isReadOnly()) {
            throw new BusinessRuleException("El estudio está " + study.getStatus().getDisplayName().toLowerCase()
                    + ": no admite cambios.");
        }
    }

    /**
     * Validates that the shift may be closed: every participating source has
     * bags or is marked as having no generation (RN-024).
     *
     * @param shift                shift to close
     * @param participatingSources participating sources: identifier to source code
     * @throws BusinessRuleException if the shift is closed or some sources are still pending
     */
    public static void validateCanClose(SamplingShift shift, Map<Long, String> participatingSources) {
        ValidationSupport.requireNonNull(shift, "turno", "El turno no puede ser nulo.");
        if (!shift.isOpen()) {
            throw new BusinessRuleException("El turno " + shift.getCode() + " ya está cerrado.");
        }
        if (participatingSources == null) {
            return;
        }
        if (participatingSources.isEmpty()) {
            throw new BusinessRuleException("No se puede cerrar el turno " + shift.getCode()
                    + ": el estudio no tiene fuentes participantes.");
        }
        List<String> pending = participatingSources.entrySet().stream()
                .filter(entry -> !shift.isSourceCovered(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted()
                .toList();
        if (!pending.isEmpty()) {
            throw new BusinessRuleException("No se puede cerrar el turno " + shift.getCode()
                    + ": faltan registros de las fuentes " + String.join(", ", pending)
                    + ". Registre sus bolsas o márquelas como \"sin generación\".");
        }
        if (shift.getPopulation() == null) {
            throw new BusinessRuleException("No se puede cerrar el turno " + shift.getCode()
                    + ": falta registrar la población.");
        }
    }

    /**
     * Validates that a closed shift may be reopened: only while the study is
     * being executed (RN-025).
     *
     * @param shift shift to reopen
     * @param study study the shift belongs to
     * @throws BusinessRuleException if the shift is open or the study is not executing
     */
    public static void validateReopen(SamplingShift shift, CharacterizationStudy study) {
        ValidationSupport.requireNonNull(shift, "turno", "El turno no puede ser nulo.");
        requireStudyInExecution(study);
        if (shift.isOpen()) {
            throw new BusinessRuleException("El turno " + shift.getCode() + " ya está abierto.");
        }
        if (!Objects.equals(shift.getStudyId(), study.getId())) {
            throw new BusinessRuleException("El turno " + shift.getCode() + " no pertenece a este estudio.");
        }
    }

    private static void requireStudyInExecution(CharacterizationStudy study) {
        if (study == null) {
            throw new InvalidDataException("estudio", "El estudio no puede ser nulo.");
        }
        if (study.getStatus() != StudyStatus.EN_EJECUCION) {
            throw new BusinessRuleException("El estudio está " + study.getStatus().getDisplayName().toLowerCase()
                    + ": solo se trabaja con turnos mientras está en ejecución.");
        }
    }

}
