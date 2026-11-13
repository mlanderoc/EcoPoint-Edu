package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CharacterizationStudy} and {@link SamplingDay} (RN-010 to RN-017).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class CharacterizationStudyTest {

    static List<LocalDate> march(int... days) {
        return java.util.Arrays.stream(days).mapToObj(day -> LocalDate.of(2026, 3, day)).toList();
    }

    static CharacterizationStudy scheduledStudy(Long id, int... days) {
        CharacterizationStudy study = new CharacterizationStudy(id, 1L);
        study.scheduleDates(march(days));
        return study;
    }

    @Test
    void ca010_newStudyHasEightDaysAndDayOneIsExcluded() {
        CharacterizationStudy study = new CharacterizationStudy(null, 1L);

        assertEquals(8, study.getDays().size());
        for (int day = 1; day <= 8; day++) {
            assertEquals(day, study.getDay(day).getDayNumber());
        }
        assertTrue(study.isDayExcluded(1));
        assertFalse(study.isDayExcluded(2));
        assertEquals(StudyStatus.PLANIFICADO, study.getStatus());
    }

    @Test
    void rn012_calculationsUseDaysTwoToEight() {
        CharacterizationStudy study = new CharacterizationStudy(null, 1L);

        assertEquals(List.of(2, 3, 4, 5, 6, 7, 8), study.getIncludedDayNumbers());
    }

    @Test
    void rn010_scheduleNeedsExactlyEightDates() {
        CharacterizationStudy study = new CharacterizationStudy(null, 1L);

        assertThrows(InvalidDataException.class, () -> study.scheduleDates(march(2, 3, 4)));
        assertThrows(InvalidDataException.class, () -> study.getDay(9));
    }

    @Test
    void rn011_startAndEndDatesComeFromDayOneAndDayEight() {
        CharacterizationStudy study = scheduledStudy(null, 2, 3, 4, 5, 6, 9, 10, 11);

        assertEquals(LocalDate.of(2026, 3, 2), study.getStartDate());
        assertEquals(LocalDate.of(2026, 3, 11), study.getEndDate());
        assertTrue(study.hasAllDatesScheduled());
        assertNull(new CharacterizationStudy(null, 1L).getStartDate());
    }

    @Test
    void ca012_overlappingRangesAreDetected() {
        CharacterizationStudy first = scheduledStudy(1L, 3, 4, 5, 6, 9, 10, 11, 14);
        CharacterizationStudy second = scheduledStudy(2L, 10, 11, 12, 13, 16, 17, 18, 21);
        CharacterizationStudy later = scheduledStudy(3L, 16, 17, 18, 19, 20, 23, 24, 25);

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
        assertFalse(first.overlaps(later));
        assertFalse(first.overlaps(new CharacterizationStudy(4L, 1L)));
    }

    @Test
    void ca013_studyCannotSkipStatuses() {
        CharacterizationStudy study = new CharacterizationStudy(null, 1L);

        assertThrows(BusinessRuleException.class, () -> study.changeStatus(StudyStatus.CERRADO));
        study.changeStatus(StudyStatus.EN_EJECUCION);
        assertEquals(StudyStatus.EN_EJECUCION, study.getStatus());
    }

    @Test
    void r1_restoreRebuildsAStoredStudyWithoutRepeatingTransitions() {
        StudyReopening reopening = new StudyReopening(7L, "Corregir el día 4", LocalDateTime.of(2026, 3, 20, 10, 0));

        CharacterizationStudy study = CharacterizationStudy.restore(5L, 1L, StudyStatus.EN_DEPURACION,
                march(2, 3, 4, 5, 6, 9, 10, 11), List.of(reopening));

        assertEquals(5L, study.getId());
        assertEquals(StudyStatus.EN_DEPURACION, study.getStatus());
        assertEquals(LocalDate.of(2026, 3, 11), study.getEndDate());
        assertEquals(List.of(reopening), study.getReopenings());
        assertTrue(study.isDayExcluded(1));
        assertThrows(InvalidDataException.class,
                () -> CharacterizationStudy.restore(5L, 1L, StudyStatus.CERRADO, march(2, 3), List.of()));
    }

    @Test
    void rn017_closedStudyCanBeReopenedWithAReasonAndItIsRecorded() {
        CharacterizationStudy study = new CharacterizationStudy(null, 1L);
        study.changeStatus(StudyStatus.EN_EJECUCION);
        study.changeStatus(StudyStatus.EN_DEPURACION);
        study.changeStatus(StudyStatus.CERRADO);
        LocalDateTime at = LocalDateTime.of(2026, 3, 20, 10, 0);

        study.reopen(7L, "Corregir pesaje del día 4", at);

        assertEquals(StudyStatus.EN_DEPURACION, study.getStatus());
        assertEquals(1, study.getReopenings().size());
        assertEquals(new StudyReopening(7L, "Corregir pesaje del día 4", at), study.getReopenings().get(0));
    }

    @Test
    void rn017_reopeningNeedsAClosedStudyAndAReason() {
        CharacterizationStudy planned = new CharacterizationStudy(null, 1L);
        assertThrows(BusinessRuleException.class, () -> planned.reopen(7L, "motivo", LocalDateTime.now()));

        CharacterizationStudy closed = new CharacterizationStudy(null, 1L);
        closed.changeStatus(StudyStatus.EN_EJECUCION);
        closed.changeStatus(StudyStatus.EN_DEPURACION);
        closed.changeStatus(StudyStatus.CERRADO);
        assertThrows(InvalidDataException.class, () -> closed.reopen(7L, " ", LocalDateTime.now()));
    }

    @Test
    void ca015_executingStudyIsCancelledWithAReasonAndItIsRecorded() {
        CharacterizationStudy study = scheduledStudy(1L, 2, 3, 4, 5, 6, 9, 10, 11);
        study.changeStatus(StudyStatus.EN_EJECUCION);
        LocalDateTime at = LocalDateTime.of(2026, 3, 5, 10, 0);

        assertThrows(InvalidDataException.class, () -> study.cancel(7L, "  ", at));
        study.cancel(7L, " Fechas equivocadas ", at);

        assertEquals(StudyStatus.CANCELADO, study.getStatus());
        assertEquals(new StudyCancellation(7L, "Fechas equivocadas", at), study.getCancellation());
        assertThrows(BusinessRuleException.class, () -> study.cancel(7L, "Otra vez", at));
    }

    @Test
    void ca016_plannedOrClosedStudyCannotBeCancelled() {
        CharacterizationStudy planned = scheduledStudy(1L, 2, 3, 4, 5, 6, 9, 10, 11);
        CharacterizationStudy closed = CharacterizationStudy.restore(2L, 1L, StudyStatus.CERRADO,
                march(2, 3, 4, 5, 6, 9, 10, 11), List.of());

        assertThrows(BusinessRuleException.class, () -> planned.cancel(7L, "Motivo", LocalDateTime.now()));
        assertThrows(BusinessRuleException.class, () -> closed.cancel(7L, "Motivo", LocalDateTime.now()));
        assertNull(closed.getCancellation());
    }

    @Test
    void ca017_onlyPlannedStudyCanBeDeleted() {
        CharacterizationStudy planned = scheduledStudy(1L, 2, 3, 4, 5, 6, 9, 10, 11);
        CharacterizationStudy executing = scheduledStudy(2L, 2, 3, 4, 5, 6, 9, 10, 11);
        executing.changeStatus(StudyStatus.EN_EJECUCION);

        planned.requireDeletable();
        BusinessRuleException error = assertThrows(BusinessRuleException.class, executing::requireDeletable);
        assertTrue(error.getMessage().contains("cancélelo"));
    }

    @Test
    void r016_restoreKeepsTheStoredCancellation() {
        StudyCancellation cancellation = new StudyCancellation(7L, "Fechas equivocadas", LocalDateTime.now());

        CharacterizationStudy study = CharacterizationStudy.restore(3L, 1L, StudyStatus.CANCELADO,
                march(2, 3, 4, 5, 6, 9, 10, 11), List.of(), cancellation);

        assertEquals(cancellation, study.getCancellation());
    }
}
