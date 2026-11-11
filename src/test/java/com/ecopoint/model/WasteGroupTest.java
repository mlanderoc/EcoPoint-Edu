package com.ecopoint.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the waste groups of Resolution 2184 of 2019 ({@link WasteGroup}).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class WasteGroupTest {

    @Test
    void rn040_thereAreExactlyFourGroups() {
        assertEquals(4, WasteGroup.values().length);
    }

    @Test
    void rn040_onlyWhiteAndGreenAreRecoverable() {
        assertTrue(WasteGroup.BLANCO.isRecoverable());
        assertTrue(WasteGroup.VERDE.isRecoverable());
        assertFalse(WasteGroup.NEGRO.isRecoverable());
        assertFalse(WasteGroup.SIN_COLOR.isRecoverable());
    }

    @Test
    void rn056_onlyNoColorGroupIsHazardous() {
        assertTrue(WasteGroup.SIN_COLOR.isHazardous());
        assertFalse(WasteGroup.BLANCO.isHazardous());
        assertFalse(WasteGroup.VERDE.isHazardous());
        assertFalse(WasteGroup.NEGRO.isHazardous());
    }

    @Test
    void rn040_groupsHaveSpanishNamesAndDescriptions() {
        assertEquals("Sin color", WasteGroup.SIN_COLOR.getDisplayName());
        assertEquals("Orgánicos aprovechables", WasteGroup.VERDE.getDescription());
    }
}
