package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of the {@code restore} methods used by the persistence layer to
 * rebuild domain objects read from the database (spec 002, plan R1).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class RestoreTest {

    private static final LocalDateTime TYPED = LocalDateTime.of(2026, 3, 4, 12, 0);
    private static final LocalDateTime CHECKED = LocalDateTime.of(2026, 3, 4, 16, 0);

    private static VerificationInfo verified() {
        return VerificationInfo.restore(VerificationStatus.VERIFICADO, 2L, TYPED, 1L, CHECKED);
    }

    @Test
    void r1_verificationInfoKeepsItsStoredStatus() {
        VerificationInfo info = verified();

        assertEquals(VerificationStatus.VERIFICADO, info.getStatus());
        assertEquals(1L, info.getVerifiedBy());
        assertEquals(CHECKED, info.getVerifiedAt());
        assertThrows(BusinessRuleException.class, () -> info.markVerified(3L, CHECKED));
    }

    @Test
    void r1_bagLabelIsParsedFromItsText() {
        assertEquals(BagLabel.of("AU1", 3, ShiftType.MANANA), BagLabel.parse("AU1-D3-M"));
        assertEquals(BagLabel.of("PAE", 8, ShiftType.TARDE).withSequence(2), BagLabel.parse("PAE-D8-T-2"));
        assertThrows(InvalidDataException.class, () -> BagLabel.parse("aula-1"));
    }

    @Test
    void r1_bagKeepsItsStoredLabelAndAtypicalMark() {
        WasteBag bag = WasteBag.restore(9L, 1L, "AU1", BagLabel.parse("AU1-D3-M-2"), new BigDecimal("70.50"),
                new BigDecimal("0.50"), new BigDecimal("0.300"), true, verified());

        assertEquals("AU1-D3-M-2", bag.getLabelText());
        assertTrue(bag.isAtypical());
        assertEquals(new BigDecimal("70.00"), bag.getNetWeight());
    }

    @Test
    void r1_quarteringKeepsItsQualityStatusAndAnnulment() {
        Quartering quartering = Quartering.restore(4L, 1L, new BigDecimal("60.00"), new BigDecimal("50.00"), verified(),
                List.of(new QuarteringResult(1L, "Papel", WasteGroup.BLANCO, new BigDecimal("47.50"))),
                QualityStatus.ANULADO, "Muestra mojada", 1L, CHECKED);

        assertEquals(QualityStatus.ANULADO, quartering.getQualityStatus());
        assertEquals("Muestra mojada", quartering.getAnnulReason());
        assertEquals(new BigDecimal("47.50"), quartering.calculateTotalClassifiedWeight());
    }

    @Test
    void r1_closedShiftIsRebuiltWithEverythingItContains() {
        WasteBag bag = WasteBag.restore(9L, 1L, "AU1", BagLabel.parse("AU1-D3-M"), new BigDecimal("12.40"),
                new BigDecimal("0.40"), new BigDecimal("0.080"), false, verified());
        ShiftPopulation population = new ShiftPopulation(5L, 480, 28, 15, verified());

        SamplingShift shift = SamplingShift.restore(3L, 1L, 3, ShiftType.MANANA, ShiftStatus.CERRADO,
                population, List.of(bag), null, null, Set.of(8L));

        assertFalse(shift.isOpen());
        assertTrue(shift.hasPopulation());
        assertEquals("AU1-D3-M", shift.getBags().get(0).getLabelText());
        assertTrue(shift.isSourceCovered(1L));
        assertTrue(shift.isSourceCovered(8L));
        assertEquals(new BigDecimal("12.00"), shift.calculateTotalNetWeight());
        assertThrows(BusinessRuleException.class, () -> shift.markSourceWithoutGeneration(2L));
    }
}
