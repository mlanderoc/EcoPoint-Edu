package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.ShiftType;
import com.ecopoint.model.VerificationInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link DensityValidator} (RN-057).
 *
 * @author craulgomez
 * @version 1.0
 */
class DensityValidatorTest {

    private static DensityMeasurement measurement(String volume, String weight) {
        return new DensityMeasurement(null, 1L, volume == null ? null : new BigDecimal(volume),
                weight == null ? null : new BigDecimal(weight),
                new VerificationInfo(10L, LocalDateTime.of(2026, 3, 4, 13, 30)));
    }

    @Test
    void rn057_validMeasurementIsAccepted() {
        assertDoesNotThrow(() -> DensityValidator.validate(measurement("0.208", "31.20")));
    }

    @Test
    void rn057_missingOrNonPositiveValuesAreRejected() {
        InvalidDataException volume = assertThrows(InvalidDataException.class,
                () -> DensityValidator.validate(measurement("0", "31.20")));
        assertEquals("volumenRecipiente", volume.getFieldName());

        InvalidDataException weight = assertThrows(InvalidDataException.class,
                () -> DensityValidator.validate(measurement("0.208", null)));
        assertEquals("pesoNeto", weight.getFieldName());
    }

    @Test
    void rn057_tooManyDecimalsAreRejected() {
        assertThrows(InvalidDataException.class, () -> DensityValidator.validate(measurement("0.2085", "31.20")));
        assertThrows(InvalidDataException.class, () -> DensityValidator.validate(measurement("0.208", "31.205")));
    }

    @Test
    void rn057_secondMeasurementInTheSameShiftIsRejected() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        assertDoesNotThrow(() -> DensityValidator.validateNoExistingDensity(shift));

        shift.registerDensity(measurement("0.208", "31.20"));

        assertThrows(BusinessRuleException.class, () -> DensityValidator.validateNoExistingDensity(shift));
    }
}
