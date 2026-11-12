package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    void auxiliaryValidationsRejectMissingEntitiesWithInvalidData() {
        User teacher = new User(1L, "Oriana", "Jurado", "oriana@unicesar.edu.co", UserRole.DOCENTE, 1L);

        assertThrows(InvalidDataException.class, () -> StudyValidator.validateNoOverlap(null, List.of()));
        assertThrows(InvalidDataException.class, () -> StudyValidator.validateStartExecution(null, 1));
        assertThrows(InvalidDataException.class, () -> StudyValidator.validateClose(null, 0, 0));
        assertThrows(InvalidDataException.class, () -> ShiftValidator.validateCanClose(null, Map.of()));
        assertThrows(InvalidDataException.class, () -> ShiftValidator.validateOpenForRegistration(null, null));
        assertThrows(InvalidDataException.class, () -> ShiftValidator.validateReopen(null, null));
        assertThrows(InvalidDataException.class, () -> QuarteringValidator.validateNoExistingQuartering(null));
        assertThrows(InvalidDataException.class, () -> QuarteringValidator.validateCategories(null, Map.of()));
        assertThrows(InvalidDataException.class, () -> QuarteringValidator.validatePermissions(null, teacher));
        assertThrows(InvalidDataException.class, () -> QuarteringValidator.validateAnnul(null, teacher));
        assertThrows(InvalidDataException.class, () -> DensityValidator.validateNoExistingDensity(null));
        assertThrows(InvalidDataException.class,
                () -> WasteBagValidator.isAtypical(null, CharacterizationSettings.defaults()));
    }

    @Test
    void rn042_groupChangeWithoutPreviousGroupDoesNotFailWithNullPointer() {
        WasteCategory category = new WasteCategory(1L, "Cartón", WasteGroup.BLANCO, null);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> WasteCategoryValidator.validateGroupChange(category, null, true));
        assertTrue(exception.getMessage().contains("sin grupo"));
    }
}
