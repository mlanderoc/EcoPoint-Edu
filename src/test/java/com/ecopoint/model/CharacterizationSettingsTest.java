package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the configurable parameters of {@link CharacterizationSettings}
 * (D8, RN-037, RN-051, RN-052).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class CharacterizationSettingsTest {

    private final CharacterizationSettings settings = CharacterizationSettings.defaults();

    @Test
    void d8_defaultValuesMatchTheSpecs() {
        assertEquals(0, settings.minSampleWeight().compareTo(new BigDecimal("45")));
        assertEquals(0, settings.maxSampleWeight().compareTo(new BigDecimal("55")));
        assertEquals(0, settings.qualityTolerancePercent().compareTo(new BigDecimal("2")));
        assertEquals(0, settings.maxBagNetWeight().compareTo(new BigDecimal("50")));
        assertEquals(0, settings.minBagDensity().compareTo(new BigDecimal("20")));
        assertEquals(0, settings.maxBagDensity().compareTo(new BigDecimal("1000")));
    }

    @Test
    void rn051_sampleWeightRangeIsInclusive() {
        assertTrue(settings.isSampleWeightInRange(new BigDecimal("45.00")));
        assertTrue(settings.isSampleWeightInRange(new BigDecimal("55.00")));
        assertFalse(settings.isSampleWeightInRange(new BigDecimal("40.00")));
        assertFalse(settings.isSampleWeightInRange(new BigDecimal("60.00")));
        assertFalse(settings.isSampleWeightInRange(null));
    }

    @Test
    void rn037_bagPlausibilityLimits() {
        assertTrue(settings.isBagNetWeightPlausible(new BigDecimal("50.00")));
        assertFalse(settings.isBagNetWeightPlausible(new BigDecimal("70.00")));
        assertFalse(settings.isBagNetWeightPlausible(BigDecimal.ZERO));
        assertFalse(settings.isBagNetWeightPlausible(new BigDecimal("-1.00")));
        assertTrue(settings.isBagDensityPlausible(new BigDecimal("150.00")));
        assertFalse(settings.isBagDensityPlausible(new BigDecimal("1200.00")));
        assertFalse(settings.isBagDensityPlausible(new BigDecimal("10.00")));
    }

    @Test
    void d8_minimumMustBeLowerThanMaximum() {
        assertThrows(InvalidDataException.class, () -> new CharacterizationSettings(
                new BigDecimal("55"), new BigDecimal("45"), new BigDecimal("2"),
                new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("1000")));
    }

    @Test
    void d8_valuesMustBePositive() {
        assertThrows(InvalidDataException.class, () -> new CharacterizationSettings(
                new BigDecimal("45"), new BigDecimal("55"), BigDecimal.ZERO,
                new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("1000")));
    }
}
