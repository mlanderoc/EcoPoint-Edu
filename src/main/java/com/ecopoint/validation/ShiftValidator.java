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
     * @throws BusinessRuleException if the study is not executing or the shift already exists
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
    }

    /**
     * Validates the population of a shift: every count is zero or more and
     * the total is greater than zero (RN-022).
     *
     * @param population population to validate
     * @throws InvalidDataException if a count is negative or nobody is present
     */
    public static void validatePopulation(ShiftPopulation population) {
        if (population == null) {
            throw new InvalidDataException("poblacion", "La población del turno no puede ser nula.");
        }
        requireNotNegative(population.getStudents(), "estudiantes");
        requireNotNegative(population.getTeachers(), "docentes");
        requireNotNegative(population.getStaff(), "personal");
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

    private static void requireNotNegative(int value, String field) {
        if (value < 0) {
            throw new InvalidDataException(field, "La cantidad de " + field + " no puede ser negativa.");
        }
    }
}
