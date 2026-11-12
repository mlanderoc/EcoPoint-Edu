package com.ecopoint.model;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VerificationInfo}, {@link VerifiableRecord} and
 * {@link Correction} (RN-060 to RN-063).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class VerificationInfoTest {

    private static final LocalDateTime TYPED = LocalDateTime.of(2026, 3, 4, 7, 30);
    private static final LocalDateTime CHECKED = LocalDateTime.of(2026, 3, 4, 15, 0);

    /** Minimal concrete record used to test the abstract base class. */
    private static final class SampleRecord extends VerifiableRecord {
        SampleRecord(Long id, VerificationInfo verification) {
            super(id, verification);
        }

        @Override
        public String getRecordType() {
            return "Bolsa";
        }
    }

    @Test
    void rn060_newRecordIsPendingAndKeepsWhoTypedIt() {
        VerificationInfo info = new VerificationInfo(10L, TYPED);

        assertEquals(VerificationStatus.PENDIENTE, info.getStatus());
        assertTrue(info.isPending());
        assertEquals(10L, info.getEnteredBy());
        assertEquals(TYPED, info.getEnteredAt());
        assertNull(info.getVerifiedBy());
        assertTrue(info.wasEnteredBy(10L));
        assertFalse(info.wasEnteredBy(20L));
    }

    @Test
    void rn061_whoAndWhenAreRequired() {
        assertThrows(InvalidDataException.class, () -> new VerificationInfo(null, TYPED));
        assertThrows(InvalidDataException.class, () -> new VerificationInfo(10L, null));
    }

    @Test
    void rn062_verificationStoresVerifierAndDate() {
        VerificationInfo info = new VerificationInfo(10L, TYPED);

        info.markVerified(20L, CHECKED);

        assertEquals(VerificationStatus.VERIFICADO, info.getStatus());
        assertEquals(20L, info.getVerifiedBy());
        assertEquals(CHECKED, info.getVerifiedAt());
        assertFalse(info.isPending());
    }

    @Test
    void rn062_recordCannotBeVerifiedTwice() {
        VerificationInfo info = new VerificationInfo(10L, TYPED);
        info.markVerified(20L, CHECKED);

        assertThrows(BusinessRuleException.class, () -> info.markVerified(30L, CHECKED));
    }

    @Test
    void rn063_verifiedRecordCanStillBeCorrected() {
        VerificationInfo info = new VerificationInfo(10L, TYPED);
        info.markVerified(20L, CHECKED);

        info.markCorrected(30L, CHECKED.plusHours(1));

        assertEquals(VerificationStatus.CORREGIDO, info.getStatus());
        assertEquals(30L, info.getVerifiedBy());
    }

    @Test
    void rn060_everyVerifiableRecordStartsPending() {
        SampleRecord record = new SampleRecord(5L, new VerificationInfo(10L, TYPED));

        assertTrue(record.isPendingVerification());
        assertThrows(InvalidDataException.class, () -> new SampleRecord(5L, null));
    }

    @Test
    void rn063_correctionKeepsOldAndNewValuesWithReason() {
        SampleRecord record = new SampleRecord(5L, new VerificationInfo(10L, TYPED));

        Correction correction = Correction.of(record, "pesoBruto", new BigDecimal("12.40"),
                new BigDecimal("12.04"), " error de digitación ", 20L, CHECKED);

        assertEquals("Bolsa", correction.recordType());
        assertEquals(5L, correction.recordId());
        assertEquals("12.40", correction.oldValue());
        assertEquals("12.04", correction.newValue());
        assertEquals("error de digitación", correction.reason());
        assertEquals(20L, correction.userId());
    }

    @Test
    void rn063_correctionNeedsAReasonAndARealChange() {
        SampleRecord record = new SampleRecord(5L, new VerificationInfo(10L, TYPED));

        assertThrows(InvalidDataException.class,
                () -> Correction.of(record, "tara", "0.40", "0.50", " ", 20L, CHECKED));
        assertThrows(InvalidDataException.class,
                () -> Correction.of(record, "tara", "0.40", "0.40", "revisión", 20L, CHECKED));
    }
}
