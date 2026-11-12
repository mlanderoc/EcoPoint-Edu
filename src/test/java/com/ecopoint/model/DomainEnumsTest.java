package com.ecopoint.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the behavior of the smaller domain enums.
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class DomainEnumsTest {

    @Test
    void rn031_shiftCodesAreTheLettersUsedInBagLabels() {
        assertEquals("M", ShiftType.MANANA.getCode());
        assertEquals("T", ShiftType.TARDE.getCode());
    }

    @Test
    void rn020_aDayHasExactlyTwoShifts() {
        assertEquals(2, ShiftType.values().length);
    }

    @Test
    void rn003_sourceTypesMatchTheSpec() {
        assertEquals(8, SourceType.values().length);
        assertEquals("Baños", SourceType.BANOS.getDisplayName());
    }

    @Test
    void rn060_onlyPendingRecordsAreStillToBeChecked() {
        assertTrue(VerificationStatus.PENDIENTE.isPending());
        assertFalse(VerificationStatus.VERIFICADO.isPending());
        assertFalse(VerificationStatus.CORREGIDO.isPending());
    }

    @Test
    void rn016_onlyNonConformingQuarteringBlocksStudyClosure() {
        assertTrue(QualityStatus.NO_CONFORME.blocksStudyClosure());
        assertFalse(QualityStatus.CONFORME.blocksStudyClosure());
        assertFalse(QualityStatus.ANULADO.blocksStudyClosure());
    }

    @Test
    void rn052_annulledQuarteringDoesNotCountForIndicators() {
        assertTrue(QualityStatus.CONFORME.countsForIndicators());
        assertTrue(QualityStatus.NO_CONFORME.countsForIndicators());
        assertFalse(QualityStatus.ANULADO.countsForIndicators());
    }
}
