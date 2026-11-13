package com.ecopoint.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the study lifecycle rules of {@link StudyStatus} (RN-014, RN-017).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class StudyStatusTest {

    @Test
    void ca013_plannedStudyCannotJumpStraightToClosed() {
        assertFalse(StudyStatus.PLANIFICADO.canTransitionTo(StudyStatus.CERRADO));
    }

    @ParameterizedTest(name = "{0} -> {1} = {2}")
    @CsvSource({
            "PLANIFICADO,   PLANIFICADO,   false",
            "PLANIFICADO,   EN_EJECUCION,  true",
            "PLANIFICADO,   EN_DEPURACION, false",
            "PLANIFICADO,   CERRADO,       false",
            "EN_EJECUCION,  PLANIFICADO,   false",
            "EN_EJECUCION,  EN_EJECUCION,  false",
            "EN_EJECUCION,  EN_DEPURACION, true",
            "EN_EJECUCION,  CERRADO,       false",
            "EN_DEPURACION, PLANIFICADO,   false",
            "EN_DEPURACION, EN_EJECUCION,  false",
            "EN_DEPURACION, EN_DEPURACION, false",
            "EN_DEPURACION, CERRADO,       true",
            "CERRADO,       PLANIFICADO,   false",
            "CERRADO,       EN_EJECUCION,  false",
            "CERRADO,       EN_DEPURACION, true",
            "CERRADO,       CERRADO,       false",
            "EN_EJECUCION,  CANCELADO,     false",
            "CANCELADO,     EN_EJECUCION,  false",
            "CANCELADO,     EN_DEPURACION, false"
    })
    void rn014_studyOnlyMovesOneStepForwardOrReopensForCleaning(
            StudyStatus from, StudyStatus to, boolean allowed) {
        assertEquals(allowed, from.canTransitionTo(to));
    }

    @Test
    void rn014_transitionToNullIsNotAllowed() {
        assertFalse(StudyStatus.EN_EJECUCION.canTransitionTo(null));
    }

    @Test
    void rn014_fieldDataIsOnlyRegisteredWhileExecuting() {
        assertFalse(StudyStatus.PLANIFICADO.allowsFieldRegistration());
        assertTrue(StudyStatus.EN_EJECUCION.allowsFieldRegistration());
        assertFalse(StudyStatus.EN_DEPURACION.allowsFieldRegistration());
        assertFalse(StudyStatus.CERRADO.allowsFieldRegistration());
    }

    @Test
    void rn014_recordsCanBeVerifiedWhileExecutingOrCleaning() {
        assertFalse(StudyStatus.PLANIFICADO.allowsVerification());
        assertTrue(StudyStatus.EN_EJECUCION.allowsVerification());
        assertTrue(StudyStatus.EN_DEPURACION.allowsVerification());
        assertFalse(StudyStatus.CERRADO.allowsVerification());
    }

    @Test
    void rn014_onlyClosedOrCancelledStudyIsReadOnly() {
        assertTrue(StudyStatus.CERRADO.isReadOnly());
        assertTrue(StudyStatus.CANCELADO.isReadOnly());
        assertFalse(StudyStatus.EN_DEPURACION.isReadOnly());
    }

    @Test
    void rn018_onlyExecutingOrCleaningStudyCanBeCancelled() {
        assertFalse(StudyStatus.PLANIFICADO.allowsCancellation());
        assertTrue(StudyStatus.EN_EJECUCION.allowsCancellation());
        assertTrue(StudyStatus.EN_DEPURACION.allowsCancellation());
        assertFalse(StudyStatus.CERRADO.allowsCancellation());
        assertFalse(StudyStatus.CANCELADO.allowsCancellation());
        assertFalse(StudyStatus.CANCELADO.allowsFieldRegistration());
        assertFalse(StudyStatus.CANCELADO.allowsVerification());
    }
}
