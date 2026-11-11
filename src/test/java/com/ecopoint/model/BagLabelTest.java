package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the bag label format of {@link BagLabel} (RN-031, CA-030).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class BagLabelTest {

    @Test
    void ca030_labelWithoutSequenceHasSourceDayAndShift() {
        assertEquals("AU-D3-M", BagLabel.of("AU", 3, ShiftType.MANANA).toString());
    }

    @Test
    void ca030_labelWithSequenceAddsTheBagNumber() {
        BagLabel label = BagLabel.of("AU", 3, ShiftType.MANANA);

        assertEquals("AU-D3-M-1", label.withSequence(1).toString());
        assertEquals("AU-D3-M-2", label.withSequence(2).toString());
    }

    @Test
    void rn031_afternoonShiftUsesLetterT() {
        assertEquals("PAE-D8-T", BagLabel.of("PAE", 8, ShiftType.TARDE).toString());
    }

    @Test
    void rn002_invalidSourceCodeIsRejected() {
        assertThrows(InvalidDataException.class, () -> BagLabel.of("au", 3, ShiftType.MANANA));
        assertThrows(InvalidDataException.class, () -> BagLabel.of("AULAS12", 3, ShiftType.MANANA));
    }

    @Test
    void rn010_dayOutsideOneToEightIsRejected() {
        assertThrows(InvalidDataException.class, () -> BagLabel.of("AU", 0, ShiftType.MANANA));
        assertThrows(InvalidDataException.class, () -> BagLabel.of("AU", 9, ShiftType.MANANA));
    }

    @Test
    void rn031_missingShiftOrNegativeSequenceIsRejected() {
        assertThrows(InvalidDataException.class, () -> BagLabel.of("AU", 3, null));
        assertThrows(InvalidDataException.class, () -> new BagLabel("AU", 3, ShiftType.MANANA, -1));
    }
}
