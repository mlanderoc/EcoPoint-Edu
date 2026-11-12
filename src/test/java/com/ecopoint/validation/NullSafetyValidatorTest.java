package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that validators reject missing entities with an exception of the
 * {@code EcoPointException} hierarchy, never with a NullPointerException
 * (AGENTS.md section 5; findings of the phase 2 review).
 *
 * @author craulgomez
 * @version 1.0
 */
class NullSafetyValidatorTest {

    @Test
    void uniquenessChecksRejectMissingEntitiesWithInvalidData() {
        assertThrows(InvalidDataException.class, () -> GenerationSourceValidator.validateUniqueCode(null, Set.of("AU")));
        assertThrows(InvalidDataException.class, () -> UserValidator.validateUniqueEmail(null, Set.of("a@b.co")));
        assertThrows(InvalidDataException.class, () -> WasteCategoryValidator.validateUniqueName(null, Set.of("Papel")));
    }

    @Test
    void rn042_groupChangeWithoutPreviousGroupDoesNotFailWithNullPointer() {
        WasteCategory category = new WasteCategory(1L, "Cartón", WasteGroup.BLANCO, null);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> WasteCategoryValidator.validateGroupChange(category, null, true));
        assertTrue(exception.getMessage().contains("sin grupo"));
    }
}
