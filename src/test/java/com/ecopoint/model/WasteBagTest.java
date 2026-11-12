package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WasteBag} and the bags of {@link SamplingShift}
 * (RN-024, RN-031, RN-032, RN-034, RN-036, CA-030, CA-032).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class WasteBagTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 12, 10);

    private static WasteBag bag(Long sourceId, String code, String gross, String tare, String volume) {
        return new WasteBag(null, sourceId, code, new BigDecimal(gross), new BigDecimal(tare),
                new BigDecimal(volume), new VerificationInfo(10L, AT));
    }

    private static SamplingShift morningOfDayThree() {
        return new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
    }

    @Test
    void ca032_netWeightAndDensityAreCalculated() {
        WasteBag bag = bag(1L, "AU", "12.40", "0.40", "0.080");

        assertEquals(new BigDecimal("12.00"), bag.getNetWeight());
        assertEquals(new BigDecimal("150.00"), bag.getDensity());
        assertEquals("Bolsa", bag.getRecordType());
    }

    @Test
    void rn036_densityIsNotCalculatedWithoutVolume() {
        WasteBag bag = new WasteBag(null, 1L, "AU", new BigDecimal("5"), BigDecimal.ZERO, null,
                new VerificationInfo(10L, AT));

        assertNull(bag.getDensity());
    }

    @Test
    void ca030_firstBagGetsLabelWithoutSequence() {
        SamplingShift shift = morningOfDayThree();

        BagLabel label = shift.addBag(bag(1L, "AU", "5", "0.2", "0.05"));

        assertEquals("AU-D3-M", label.toString());
    }

    @Test
    void ca030_firstBagKeepsItsLabelAndNextBagsAreNumbered() {
        SamplingShift shift = morningOfDayThree();
        WasteBag first = bag(1L, "AU", "5", "0.2", "0.05");
        WasteBag second = bag(1L, "AU", "6", "0.2", "0.05");
        WasteBag third = bag(1L, "AU", "7", "0.2", "0.05");

        shift.addBag(first);
        shift.addBag(second);
        shift.addBag(third);

        assertEquals("AU-D3-M", first.getLabelText());
        assertEquals("AU-D3-M-2", second.getLabelText());
        assertEquals("AU-D3-M-3", third.getLabelText());
    }

    @Test
    void rn032_labelsAreUniqueWithinTheShift() {
        SamplingShift shift = morningOfDayThree();
        shift.addBag(bag(1L, "AU", "5", "0.2", "0.05"));
        shift.addBag(bag(1L, "AU", "6", "0.2", "0.05"));
        shift.addBag(bag(2L, "BA", "3", "0.1", "0.04"));

        long distinct = shift.getBags().stream().map(WasteBag::getLabelText).distinct().count();
        assertEquals(3, distinct);
    }

    @Test
    void rn024_sourceWithBagsIsCoveredAndCannotBeMarkedWithoutGeneration() {
        SamplingShift shift = morningOfDayThree();
        shift.addBag(bag(1L, "AU", "5", "0.2", "0.05"));

        assertTrue(shift.isSourceCovered(1L));
        assertFalse(shift.isSourceCovered(2L));
        assertThrows(BusinessRuleException.class, () -> shift.markSourceWithoutGeneration(1L));
    }

    @Test
    void rn024_sourceMarkedWithoutGenerationCannotReceiveBags() {
        SamplingShift shift = morningOfDayThree();
        shift.markSourceWithoutGeneration(2L);

        assertThrows(BusinessRuleException.class, () -> shift.addBag(bag(2L, "BA", "3", "0.1", "0.04")));
    }

    @Test
    void rn051_totalNetWeightAddsAllBags() {
        SamplingShift shift = morningOfDayThree();
        shift.addBag(bag(1L, "AU", "12.40", "0.40", "0.080"));
        shift.addBag(bag(2L, "BA", "3.25", "0.25", "0.040"));

        assertEquals(new BigDecimal("15.00"), shift.calculateTotalNetWeight());
    }

    @Test
    void rn021_closedShiftDoesNotReceiveBags() {
        SamplingShift shift = morningOfDayThree();
        shift.close();

        assertThrows(BusinessRuleException.class, () -> shift.addBag(bag(1L, "AU", "5", "0.2", "0.05")));
    }
}
