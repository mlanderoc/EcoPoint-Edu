package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.QualityControlException;
import com.ecopoint.model.CharacterizationStudy;
import com.ecopoint.model.Institution;
import com.ecopoint.model.StudyStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link StudyValidator} (RN-011, RN-013, RN-015, RN-016,
 * CA-011, CA-012, CA-014).
 *
 * @author craulgomez
 * @version 1.0
 */
class StudyValidatorTest {

    private static List<LocalDate> march(int... days) {
        return Arrays.stream(days).mapToObj(day -> LocalDate.of(2026, 3, day)).toList();
    }

    private static CharacterizationStudy study(Long id, Long institutionId, int... days) {
        CharacterizationStudy study = new CharacterizationStudy(id, institutionId);
        study.scheduleDates(march(days));
        return study;
    }

    private static CharacterizationStudy studyInCleaning() {
        CharacterizationStudy study = study(1L, 1L, 2, 3, 4, 5, 6, 9, 10, 11);
        study.changeStatus(StudyStatus.EN_EJECUCION);
        study.changeStatus(StudyStatus.EN_DEPURACION);
        return study;
    }

    @Test
    void rn011_increasingDatesAreAccepted() {
        assertDoesNotThrow(() -> StudyValidator.validate(study(null, 1L, 2, 3, 4, 5, 6, 9, 10, 11)));
    }

    @Test
    void ca011_dayThreeBeforeDayTwoIsRejected() {
        CharacterizationStudy study = study(null, 1L, 2, 5, 4, 6, 9, 10, 11, 12);

        InvalidDataException exception =
                assertThrows(InvalidDataException.class, () -> StudyValidator.validate(study));
        assertEquals("fechas", exception.getFieldName());
    }

    @Test
    void rn011_twoDaysOnTheSameDateAreRejected() {
        assertThrows(InvalidDataException.class,
                () -> StudyValidator.validate(study(null, 1L, 2, 3, 3, 4, 5, 6, 9, 10)));
    }

    @Test
    void rn011_planningWithoutDatesIsAllowed() {
        assertDoesNotThrow(() -> StudyValidator.validate(new CharacterizationStudy(null, 1L)));
    }

    @Test
    void rn010_studyWithoutInstitutionIsRejected() {
        assertThrows(InvalidDataException.class,
                () -> StudyValidator.validate(new CharacterizationStudy(null, null)));
    }

    @Test
    void ca012_overlappingStudyInSameInstitutionIsRejected() {
        CharacterizationStudy existing = study(1L, 1L, 3, 4, 5, 6, 9, 10, 11, 14);
        CharacterizationStudy newStudy = study(null, 1L, 10, 11, 12, 13, 16, 17, 18, 21);

        assertThrows(BusinessRuleException.class,
                () -> StudyValidator.validateNoOverlap(newStudy, List.of(existing)));
    }

    @Test
    void ca015_cancelledStudyFreesItsDates() {
        CharacterizationStudy cancelled = study(1L, 1L, 3, 4, 5, 6, 9, 10, 11, 12);
        cancelled.changeStatus(StudyStatus.EN_EJECUCION);
        cancelled.cancel(7L, "Fechas equivocadas", LocalDateTime.of(2026, 3, 5, 10, 0));
        CharacterizationStudy replacement = study(null, 1L, 3, 4, 5, 6, 9, 10, 11, 12);

        assertDoesNotThrow(() -> StudyValidator.validateNoOverlap(replacement, List.of(cancelled)));
    }

    @Test
    void ca005_inactiveInstitutionDoesNotAcceptNewStudies() {
        Institution institution = new Institution(1L, "I.E. Prudencia Daza", "Valledupar");
        institution.deactivate();

        assertThrows(BusinessRuleException.class, () -> StudyValidator.validateInstitutionAcceptsStudies(institution));
        institution.activate();
        assertDoesNotThrow(() -> StudyValidator.validateInstitutionAcceptsStudies(institution));
    }

    @Test
    void rn013_overlapWithAnotherInstitutionOrWithItselfIsAllowed() {
        CharacterizationStudy otherInstitution = study(1L, 2L, 3, 4, 5, 6, 9, 10, 11, 14);
        CharacterizationStudy rescheduled = study(5L, 1L, 10, 11, 12, 13, 16, 17, 18, 21);
        CharacterizationStudy sameStudyStored = study(5L, 1L, 9, 10, 11, 12, 13, 16, 17, 18);

        assertDoesNotThrow(() -> StudyValidator.validateNoOverlap(rescheduled,
                List.of(otherInstitution, sameStudyStored)));
    }

    @Test
    void rn015_studyStartsWithAllDatesAndAtLeastOneSource() {
        assertDoesNotThrow(() -> StudyValidator.validateStartExecution(study(1L, 1L, 2, 3, 4, 5, 6, 9, 10, 11), 3));
    }

    @Test
    void rn015_studyWithoutDatesOrSourcesCannotStart() {
        assertThrows(BusinessRuleException.class,
                () -> StudyValidator.validateStartExecution(new CharacterizationStudy(1L, 1L), 3));
        assertThrows(BusinessRuleException.class,
                () -> StudyValidator.validateStartExecution(study(1L, 1L, 2, 3, 4, 5, 6, 9, 10, 11), 0));
    }

    @Test
    void ca014_studyWithPendingRecordsCannotCloseAndSaysHowMany() {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> StudyValidator.validateClose(studyInCleaning(), 4, 0));

        assertEquals("No se puede cerrar el estudio: quedan 4 registros pendientes de verificación.",
                exception.getMessage());
    }

    @Test
    void rn016_studyWithNonConformingQuarteringCannotClose() {
        QualityControlException exception = assertThrows(QualityControlException.class,
                () -> StudyValidator.validateClose(studyInCleaning(), 0, 1));

        assertEquals("ERR_QUALITY_CONTROL", exception.getErrorCode());
    }

    @Test
    void rn016_cleanStudyCanClose() {
        assertDoesNotThrow(() -> StudyValidator.validateClose(studyInCleaning(), 0, 0));
    }

    @Test
    void rn014_onlyAStudyInCleaningCanClose() {
        assertThrows(BusinessRuleException.class,
                () -> StudyValidator.validateClose(new CharacterizationStudy(1L, 1L), 0, 0));
    }
}
