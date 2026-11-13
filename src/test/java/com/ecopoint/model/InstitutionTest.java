package com.ecopoint.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Institution} (RN-006).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
class InstitutionTest {

    @Test
    void rn006_institutionIsDeactivatedAndActivatedAgain() {
        Institution institution = new Institution(1L, "I.E. Prudencia Daza", "Valledupar");

        assertTrue(institution.isActive());
        institution.deactivate();
        assertFalse(institution.isActive());
        institution.activate();
        assertTrue(institution.isActive());
    }
}
