package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SamplingShift} and {@link ShiftPopulation} (RN-020 to RN-025).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class SamplingShiftTest {

    private static ShiftPopulation population(int students, int teachers, int staff) {
        return new ShiftPopulation(null, students, teachers, staff,
                new VerificationInfo(10L, LocalDateTime.of(2026, 3, 4, 6, 30)));
    }

    @Test
    void rn020_newShiftIsOpenAndHasItsCode() {
        SamplingShift shift = new SamplingShift(null, 1L, 3, ShiftType.MANANA);

        assertTrue(shift.isOpen());
        assertEquals("D3-M", shift.getCode());
        assertFalse(shift.hasPopulation());
    }

    @Test
    void rn020_shiftNeedsStudyValidDayAndType() {
        assertThrows(InvalidDataException.class, () -> new SamplingShift(null, null, 3, ShiftType.MANANA));
        assertThrows(InvalidDataException.class, () -> new SamplingShift(null, 1L, 9, ShiftType.MANANA));
        assertThrows(InvalidDataException.class, () -> new SamplingShift(null, 1L, 3, null));
    }

    @Test
    void rn022_populationTotalAddsTheThreeGroups() {
        assertEquals(463, population(420, 25, 18).getTotal());
        assertEquals("Población del turno", population(1, 0, 0).getRecordType());
        assertTrue(population(1, 0, 0).isPendingVerification());
    }

    @Test
    void rn022_populationIsRegisteredOnlyOnce() {
        SamplingShift shift = new SamplingShift(null, 1L, 3, ShiftType.MANANA);
        shift.registerPopulation(population(420, 25, 18));

        assertTrue(shift.hasPopulation());
        assertThrows(BusinessRuleException.class, () -> shift.registerPopulation(population(400, 20, 15)));
    }

    @Test
    void rn021_closedShiftDoesNotAcceptChanges() {
        SamplingShift shift = new SamplingShift(null, 1L, 3, ShiftType.TARDE);
        shift.close();

        assertFalse(shift.isOpen());
        assertThrows(BusinessRuleException.class, () -> shift.registerPopulation(population(10, 1, 1)));
        assertThrows(BusinessRuleException.class, () -> shift.markSourceWithoutGeneration(5L));
        assertThrows(BusinessRuleException.class, shift::close);
    }

    @Test
    void rn024_sourceMarkedWithoutGenerationIsCovered() {
        SamplingShift shift = new SamplingShift(null, 1L, 3, ShiftType.MANANA);

        shift.markSourceWithoutGeneration(5L);
        assertTrue(shift.isSourceCovered(5L));

        shift.unmarkSourceWithoutGeneration(5L);
        assertFalse(shift.isSourceCovered(5L));
    }

    @Test
    void rn025_closedShiftCanBeReopenedButAnOpenOneCannot() {
        SamplingShift shift = new SamplingShift(null, 1L, 3, ShiftType.MANANA);

        assertThrows(BusinessRuleException.class, shift::reopen);
        shift.close();
        shift.reopen();
        assertTrue(shift.isOpen());
    }
}
