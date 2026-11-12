package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WasteCategoryValidator} and {@link WasteCategory} (RN-040 to RN-042).
 *
 * @author craulgomez
 * @version 1.0
 */
class WasteCategoryValidatorTest {

    private static WasteCategory cardboard() {
        return new WasteCategory(1L, "Cartón", WasteGroup.BLANCO, "Cajas y empaques de cartón");
    }

    @Test
    void rn041_validSubcategoryIsAccepted() {
        assertDoesNotThrow(() -> WasteCategoryValidator.validate(cardboard()));
    }

    @Test
    void rn041_subcategoryMustBelongToAGroup() {
        WasteCategory category = new WasteCategory(null, "Cartón", null, null);

        assertThrows(InvalidDataException.class, () -> WasteCategoryValidator.validate(category));
    }

    @Test
    void rn041_subcategoryNameIsRequired() {
        WasteCategory category = new WasteCategory(null, "  ", WasteGroup.BLANCO, null);

        assertThrows(InvalidDataException.class, () -> WasteCategoryValidator.validate(category));
    }

    @Test
    void rn041_duplicatedNameIsRejectedIgnoringCaseAndSpaces() {
        WasteCategory category = new WasteCategory(null, " cartón ", WasteGroup.BLANCO, null);

        assertThrows(BusinessRuleException.class,
                () -> WasteCategoryValidator.validateUniqueName(category, Set.of("Papel", "Cartón")));
    }

    @Test
    void rn041_newNameIsAccepted() {
        assertDoesNotThrow(() -> WasteCategoryValidator.validateUniqueName(cardboard(), Set.of("Papel", "Vidrio")));
    }

    @Test
    void rn042_subcategoryWithResultsCannotChangeGroup() {
        WasteCategory category = cardboard();
        category.setGroup(WasteGroup.NEGRO);

        assertThrows(BusinessRuleException.class,
                () -> WasteCategoryValidator.validateGroupChange(category, WasteGroup.BLANCO, true));
    }

    @Test
    void rn042_subcategoryWithoutResultsCanChangeGroup() {
        WasteCategory category = cardboard();
        category.setGroup(WasteGroup.NEGRO);

        assertDoesNotThrow(() -> WasteCategoryValidator.validateGroupChange(category, WasteGroup.BLANCO, false));
    }

    @Test
    void rn042_subcategoryWithResultsCanBeEditedIfGroupStaysTheSame() {
        WasteCategory category = cardboard();
        category.setDescription("Cartón corrugado y plegadiza");

        assertDoesNotThrow(() -> WasteCategoryValidator.validateGroupChange(category, WasteGroup.BLANCO, true));
    }

    @Test
    void rn042_subcategoryWithResultsCannotBeDeletedOnlyDeactivated() {
        WasteCategory category = cardboard();

        assertThrows(BusinessRuleException.class, () -> WasteCategoryValidator.validateDeletion(category, true));
        category.deactivate();
        assertFalse(category.isActive());
    }

    @Test
    void rn040_subcategoryKnowsIfItIsHazardousOrRecoverable() {
        WasteCategory batteries = new WasteCategory(2L, "Pilas", WasteGroup.SIN_COLOR, null);

        assertTrue(batteries.isHazardous());
        assertFalse(batteries.isRecoverable());
        assertTrue(cardboard().isRecoverable());
    }
}
