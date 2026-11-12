package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.QualityControlException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.SamplingDay;
import com.ecopoint.model.StudyStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Stateless validator for characterization studies (RN-010 to RN-016),
 * applying GRASP Pure Fabrication and SOLID SRP. Counts and other studies
 * come as parameters because the domain does not query the database.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class StudyValidator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private StudyValidator() {
    }

    /**
     * Validates the basic data of a study and the order of the dates already
     * scheduled: each scheduled day must be later than the previous scheduled
     * day (RN-011). Days without date are allowed while planning.
     *
     * @param study study to validate
     * @throws InvalidDataException if the study, its institution or its date order is invalid
     */
    public static void validate(CharacterizationStudy study) {
        if (study == null) {
            throw new InvalidDataException("estudio", "El estudio no puede ser nulo.");
        }
        if (study.getInstitutionId() == null) {
            throw new InvalidDataException("institucion", "El estudio debe pertenecer a una institución.");
        }
        SamplingDay previous = null;
        for (SamplingDay day : study.getDays()) {
            if (!day.isScheduled()) {
                continue;
            }
            if (previous != null && !day.getDate().isAfter(previous.getDate())) {
                throw new InvalidDataException("fechas", "La fecha del día " + day.getDayNumber()
                        + " (" + format(day.getDate()) + ") debe ser posterior a la del día "
                        + previous.getDayNumber() + " (" + format(previous.getDate()) + ").");
            }
            previous = day;
        }
    }

    /**
     * Validates that the study does not overlap with another study of the same
     * institution (RN-013). The study itself, studies of other institutions and
     * studies without dates are ignored.
     *
     * @param study  study being created or rescheduled
     * @param others other studies to compare with
     * @throws BusinessRuleException if the dates overlap with another study
     */
    public static void validateNoOverlap(CharacterizationStudy study, List<CharacterizationStudy> others) {
        ValidationSupport.requireNonNull(study, "estudio", "El estudio no puede ser nulo.");
        if (others == null) {
            return;
        }
        for (CharacterizationStudy other : others) {
            boolean sameStudy = study.getId() != null && Objects.equals(study.getId(), other.getId());
            boolean sameInstitution = Objects.equals(study.getInstitutionId(), other.getInstitutionId());
            if (!sameStudy && sameInstitution && study.overlaps(other)) {
                throw new BusinessRuleException("Las fechas del estudio se cruzan con otro estudio de la misma "
                        + "institución (del " + format(other.getStartDate()) + " al "
                        + format(other.getEndDate()) + ").");
            }
        }
    }

    /**
     * Validates that a planned study may start its execution (RN-015): it
     * needs its 8 dates and at least one active generation source.
     *
     * @param study         study to start
     * @param activeSources number of active sources taking part in the study
     * @throws BusinessRuleException if the study is not planned, lacks dates or has no sources
     */
    public static void validateStartExecution(CharacterizationStudy study, int activeSources) {
        ValidationSupport.requireNonNull(study, "estudio", "El estudio no puede ser nulo.");
        requireTransition(study, StudyStatus.EN_EJECUCION);
        if (!study.hasAllDatesScheduled()) {
            throw new BusinessRuleException("No se puede iniciar el estudio: faltan fechas por definir en "
                    + "algunos de los " + CharacterizationStudy.SAMPLING_DAYS + " días.");
        }
        if (activeSources < 1) {
            throw new BusinessRuleException("No se puede iniciar el estudio: debe participar al menos una "
                    + "fuente de generación activa.");
        }
    }

    /**
     * Validates that a study in data cleaning may be closed (RN-016): no
     * record pending verification and no unresolved non-conforming quartering.
     *
     * @param study                  study to close
     * @param pendingRecords         records still pending verification
     * @param unresolvedQuarterings  non-conforming quarterings not yet re-weighed or annulled
     * @throws BusinessRuleException   if the study is not in data cleaning or has pending records
     * @throws QualityControlException if non-conforming quarterings remain
     */
    public static void validateClose(CharacterizationStudy study, int pendingRecords, int unresolvedQuarterings) {
        ValidationSupport.requireNonNull(study, "estudio", "El estudio no puede ser nulo.");
        requireTransition(study, StudyStatus.CERRADO);
        if (pendingRecords > 0) {
            throw new BusinessRuleException("No se puede cerrar el estudio: quedan " + pendingRecords
                    + (pendingRecords == 1 ? " registro pendiente" : " registros pendientes")
                    + " de verificación.");
        }
        if (unresolvedQuarterings > 0) {
            throw new QualityControlException("No se puede cerrar el estudio: quedan " + unresolvedQuarterings
                    + (unresolvedQuarterings == 1 ? " cuarteo no conforme" : " cuarteos no conformes")
                    + " sin resolver.");
        }
    }

    private static void requireTransition(CharacterizationStudy study, StudyStatus target) {
        if (!study.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException("El estudio no puede pasar de " + study.getStatus().getDisplayName()
                    + " a " + target.getDisplayName() + ".");
        }
    }

    private static String format(LocalDate date) {
        return date == null ? "sin fecha" : DATE_FORMAT.format(date);
    }
}
