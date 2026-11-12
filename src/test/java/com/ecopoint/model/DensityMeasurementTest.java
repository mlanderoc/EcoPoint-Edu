package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DensityMeasurement} and the density of {@link SamplingShift} (RN-057).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class DensityMeasurementTest {

    private static DensityMeasurement measurement(String volume, String weight) {
        return new DensityMeasurement(null, 1L, volume == null ? null : new BigDecimal(volume),
                weight == null ? null : new BigDecimal(weight),
                new VerificationInfo(10L, LocalDateTime.of(2026, 3, 4, 13, 30)));
    }

    @Test
    void rn057_densityOfAFiftyFiveGallonDrum() {
        DensityMeasurement drum = measurement("0.208", "31.20");

        assertEquals(new BigDecimal("150.00"), drum.getDensity());
        assertEquals("Medición de densidad", drum.getRecordType());
        assertTrue(drum.isPendingVerification());
    }

    @Test
    void rn057_densityIsNotCalculatedWithoutVolume() {
        assertNull(measurement(null, "31.20").getDensity());
        assertNull(measurement("0", "31.20").getDensity());
    }

    @Test
    void rn057_shiftAcceptsOnlyOneDensityMeasurement() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        shift.registerDensity(measurement("0.208", "31.20"));

        assertTrue(shift.hasDensity());
        assertThrows(BusinessRuleException.class, () -> shift.registerDensity(measurement("0.208", "30.00")));
    }

    @Test
    void rn021_closedShiftDoesNotAcceptDensity() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        shift.close();

        assertThrows(BusinessRuleException.class, () -> shift.registerDensity(measurement("0.208", "31.20")));
    }
}
