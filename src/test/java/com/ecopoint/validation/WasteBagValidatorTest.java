package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.VerificationInfo;
import com.ecopoint.model.WasteBag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WasteBagValidator} (RN-030 to RN-037, CA-031, CA-033)
 * and the bag side of CA-023.
 *
 * @author craulgomez
 * @version 1.0
 */
class WasteBagValidatorTest {

    private static final Set<Long> SOURCES = Set.of(1L, 2L);
    private static final CharacterizationSettings SETTINGS = CharacterizationSettings.defaults();

    private static WasteBag bag(Long sourceId, String gross, String tare, String volume) {
        return new WasteBag(null, sourceId, "AU",
                gross == null ? null : new BigDecimal(gross),
                tare == null ? null : new BigDecimal(tare),
                volume == null ? null : new BigDecimal(volume),
                new VerificationInfo(10L, LocalDateTime.of(2026, 3, 4, 12, 10)));
    }

    @Test
    void rn033_validBagIsAccepted() {
        assertDoesNotThrow(() -> WasteBagValidator.validate(bag(1L, "12.40", "0.40", "0.080"), SOURCES));
    }

    @Test
    void ca031_tareGreaterThanGrossWeightIsRejected() {
        InvalidDataException exception = assertThrows(InvalidDataException.class,
                () -> WasteBagValidator.validate(bag(1L, "3.20", "3.50", "0.050"), SOURCES));

        assertEquals("tara", exception.getFieldName());
    }

    @Test
    void rn033_missingOrNegativeValuesAreRejected() {
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, null, "0.4", "0.08"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "5", "-0.1", "0.08"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "5", null, "0.08"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "5", "0.4", "0"), SOURCES));
    }

    @Test
    void rn033_tooManyDecimalsAreRejected() {
        assertThrows(InvalidDataException.class,
                () -> WasteBagValidator.validate(bag(1L, "12.405", "0.40", "0.080"), SOURCES));
        assertThrows(InvalidDataException.class,
                () -> WasteBagValidator.validate(bag(1L, "12.40", "0.40", "0.0805"), SOURCES));
    }

    @Test
    void rn030_bagFromNonParticipatingSourceIsRejected() {
        assertThrows(BusinessRuleException.class,
                () -> WasteBagValidator.validate(bag(9L, "12.40", "0.40", "0.080"), SOURCES));
    }

    @Test
    void ca033_heavyBagIsAcceptedButMarkedAtypical() {
        WasteBag heavy = bag(1L, "55.50", "0.50", "0.300");

        assertDoesNotThrow(() -> WasteBagValidator.validate(heavy, SOURCES));
        assertTrue(WasteBagValidator.isAtypical(heavy, SETTINGS));
    }

    @Test
    void rn037_normalBagIsNotAtypicalButImplausibleDensityIs() {
        assertFalse(WasteBagValidator.isAtypical(bag(1L, "12.40", "0.40", "0.080"), SETTINGS));
        assertTrue(WasteBagValidator.isAtypical(bag(1L, "12.40", "0.40", "0.005"), SETTINGS));
    }

    @Test
    void ca023_shiftCanCloseWhenTheSourceHasBags() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        shift.registerPopulation(new com.ecopoint.model.ShiftPopulation(null, 300, 12, 6,
                new com.ecopoint.model.VerificationInfo(11L, java.time.LocalDateTime.of(2026, 3, 4, 8, 0))));
        shift.addBag(bag(1L, "12.40", "0.40", "0.080"));
        shift.markSourceWithoutGeneration(2L);

        assertDoesNotThrow(() -> ShiftValidator.validateCanClose(shift, Map.of(1L, "AU", 2L, "BA")));
    }

    @Test
    void rev1_bagValuesOutsideTheirLogicalRangeAreRejected() {
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "60.50", "0.40", "0.200"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "12.40", "5.50", "0.080"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "12.40", "0.40", "0.004"), SOURCES));
        assertThrows(InvalidDataException.class, () -> WasteBagValidator.validate(bag(1L, "12.40", "0.40", "0.350"), SOURCES));
    }

    @Test
    void rev1_bagWithAnAbsurdDensityIsRejected() {
        InvalidDataException error = assertThrows(InvalidDataException.class,
                () -> WasteBagValidator.validate(bag(1L, "0.50", "0.40", "0.300"), SOURCES));
        assertTrue(error.getMessage().endsWith(
                "La densidad de la bolsa debe estar entre 10 y 1500 kg/m³: revise el peso y el volumen."));
    }

    @Test
    void rev1_valuesOnTheLimitsAreAccepted() {
        assertDoesNotThrow(() -> WasteBagValidator.validate(bag(1L, "60.00", "5.00", "0.300"), SOURCES));
    }
}
