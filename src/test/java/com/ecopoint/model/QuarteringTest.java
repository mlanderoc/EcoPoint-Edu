package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Quartering} and {@link QuarteringResult}
 * (RN-050 to RN-054, CA-052 to CA-054).
 *
 * @author EstefaniaMarquez
 * @version 2.0
 */
class QuarteringTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("2");
    private static final LocalDateTime AT = LocalDateTime.of(2026, 3, 4, 13, 0);

    private static Quartering sampleOf(String sample, String... groupAndWeights) {
        Quartering quartering = new Quartering(null, 1L, new BigDecimal("120.00"), new BigDecimal(sample),
                new VerificationInfo(10L, AT));
        for (int i = 0; i < groupAndWeights.length; i += 2) {
            WasteGroup group = WasteGroup.valueOf(groupAndWeights[i]);
            quartering.addResult(new QuarteringResult((long) i, "Sub " + i, group, new BigDecimal(groupAndWeights[i + 1])));
        }
        return quartering;
    }

    @Test
    void ca052_differenceOfOnePointSixPercentIsConforming() {
        Quartering quartering = sampleOf("50.00", "BLANCO", "20.00", "VERDE", "29.20");

        assertEquals(new BigDecimal("49.20"), quartering.calculateTotalClassifiedWeight());
        assertEquals(new BigDecimal("0.80"), quartering.calculateDifferenceKg());
        assertEquals(new BigDecimal("1.60"), quartering.calculateDifferencePercent());
        assertEquals(QualityStatus.CONFORME, quartering.evaluateQuality(TOLERANCE));
    }

    @Test
    void ca053_differenceOfThreePercentIsNotConforming() {
        Quartering quartering = sampleOf("50.00", "BLANCO", "20.00", "VERDE", "28.50");

        assertEquals(new BigDecimal("3.00"), quartering.calculateDifferencePercent());
        assertEquals(QualityStatus.NO_CONFORME, quartering.evaluateQuality(TOLERANCE));
    }

    @Test
    void rn052_exactlyTwoPercentIsStillConforming() {
        Quartering quartering = sampleOf("50.00", "NEGRO", "49.00");

        assertEquals(QualityStatus.CONFORME, quartering.evaluateQuality(TOLERANCE));
    }

    @Test
    void rn052_classifiedWeightAboveTheSampleAlsoCounts() {
        Quartering quartering = sampleOf("50.00", "NEGRO", "51.50");

        assertEquals(new BigDecimal("3.00"), quartering.calculateDifferencePercent());
        assertEquals(QualityStatus.NO_CONFORME, quartering.evaluateQuality(TOLERANCE));
    }

    @Test
    void ca054_compositionBySubcategoryAndByGroup() {
        Quartering quartering = new Quartering(null, 1L, new BigDecimal("120.00"), new BigDecimal("50.00"),
                new VerificationInfo(10L, AT));
        QuarteringResult paper = new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal("10"));
        QuarteringResult food = new QuarteringResult(2L, "Restos de comida", WasteGroup.VERDE, new BigDecimal("25"));
        QuarteringResult foam = new QuarteringResult(3L, "Icopor", WasteGroup.NEGRO, new BigDecimal("15"));
        quartering.addResult(paper);
        quartering.addResult(food);
        quartering.addResult(foam);

        quartering.calculateCompositionPercentages();
        Map<WasteGroup, BigDecimal> groups = quartering.calculateGroupComposition();

        assertEquals(new BigDecimal("20.00"), paper.getPercentage());
        assertEquals(new BigDecimal("50.00"), food.getPercentage());
        assertEquals(new BigDecimal("30.00"), foam.getPercentage());
        assertEquals(new BigDecimal("20.00"), groups.get(WasteGroup.BLANCO));
        assertEquals(new BigDecimal("50.00"), groups.get(WasteGroup.VERDE));
        assertEquals(new BigDecimal("30.00"), groups.get(WasteGroup.NEGRO));
        assertEquals(new BigDecimal("0.00"), groups.get(WasteGroup.SIN_COLOR));
    }

    @Test
    void rn052_nonConformingQuarteringCanBeAnnulledWithAReason() {
        Quartering quartering = sampleOf("50.00", "BLANCO", "20.00", "VERDE", "28.50");
        quartering.evaluateQuality(TOLERANCE);

        quartering.annul("Muestra contaminada con agua", 20L, AT);

        assertEquals(QualityStatus.ANULADO, quartering.getQualityStatus());
        assertEquals("Muestra contaminada con agua", quartering.getAnnulReason());
        assertEquals(QualityStatus.ANULADO, quartering.evaluateQuality(TOLERANCE));
    }

    @Test
    void rn052_onlyNonConformingQuarteringIsAnnulledAndNeedsAReason() {
        Quartering conforming = sampleOf("50.00", "NEGRO", "49.50");
        conforming.evaluateQuality(TOLERANCE);
        assertThrows(BusinessRuleException.class, () -> conforming.annul("motivo", 20L, AT));

        Quartering nonConforming = sampleOf("50.00", "NEGRO", "45.00");
        nonConforming.evaluateQuality(TOLERANCE);
        assertThrows(InvalidDataException.class, () -> nonConforming.annul(" ", 20L, AT));
    }

    @Test
    void rn056_hazardousResultsAreDetected() {
        assertTrue(sampleOf("50.00", "SIN_COLOR", "0.30", "NEGRO", "49.50").hasHazardousResults());
        assertFalse(sampleOf("50.00", "SIN_COLOR", "0", "NEGRO", "49.50").hasHazardousResults());
    }

    @Test
    void rn051_fullSampleWhenTheWholeShiftIsClassified() {
        Quartering full = new Quartering(null, 1L, new BigDecimal("30.00"), new BigDecimal("30"),
                new VerificationInfo(10L, AT));

        assertTrue(full.isFullSample());
        assertFalse(sampleOf("50.00").isFullSample());
        assertEquals("Cuarteo", full.getRecordType());
    }

    @Test
    void rn050_shiftAcceptsOnlyOneQuartering() {
        SamplingShift shift = new SamplingShift(1L, 1L, 3, ShiftType.MANANA);
        shift.registerQuartering(sampleOf("50.00"));

        assertTrue(shift.hasQuartering());
        assertThrows(BusinessRuleException.class, () -> shift.registerQuartering(sampleOf("50.00")));
    }
}
