package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;

import java.util.Set;

/**
 * Stateless validator for waste subcategories (RN-041, RN-042), applying GRASP
 * Pure Fabrication and SOLID SRP. Data about other subcategories and about
 * existing quartering results is received as parameters.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class WasteCategoryValidator {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private WasteCategoryValidator() {
    }

    /**
     * Validates the data of a subcategory: name, group and description length.
     *
     * @param category subcategory to validate
     * @throws InvalidDataException if a field is missing or too long
     */
    public static void validate(WasteCategory category) {
        if (category == null) {
            throw new InvalidDataException("subcategoria", "La subcategoría de residuo no puede ser nula.");
        }
        if (category.getName() == null || category.getName().isBlank()) {
            throw new InvalidDataException("nombre", "El nombre de la subcategoría es obligatorio.");
        }
        if (category.getName().trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidDataException("nombre",
                    "El nombre de la subcategoría no puede superar " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (category.getGroup() == null) {
            throw new InvalidDataException("grupo",
                    "La subcategoría debe pertenecer a un grupo: Blanco, Verde, Negro o Sin color.");
        }
        if (category.getDescription() != null && category.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidDataException("descripcion",
                    "La descripción no puede superar " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
    }

    /**
     * Validates that no other subcategory has the same name, ignoring spaces
     * and upper or lower case (RN-041). When editing, the caller must exclude
     * the subcategory's own current name from the set.
     *
     * @param category      subcategory being created or edited
     * @param existingNames names of the other subcategories
     * @throws InvalidDataException if the subcategory is null
     * @throws BusinessRuleException if the name is already taken
     */
    public static void validateUniqueName(WasteCategory category, Set<String> existingNames) {
        if (category == null) {
            throw new InvalidDataException("subcategoria", "La subcategoría de residuo no puede ser nula.");
        }
        if (existingNames == null) {
            return;
        }
        String normalized = WasteCategory.normalizeName(category.getName());
        boolean taken = existingNames.stream()
                .map(WasteCategory::normalizeName)
                .anyMatch(normalized::equals);
        if (taken) {
            throw new BusinessRuleException("Ya existe una subcategoría llamada \""
                    + category.getName().trim() + "\".");
        }
    }

    /**
     * Validates that a subcategory with quartering results does not change its
     * group (RN-042), because that would alter past compositions.
     *
     * @param category      subcategory with its new group
     * @param previousGroup group the subcategory had before the edit
     * @param hasResults    whether the subcategory already has quartering results
     * @throws BusinessRuleException if the group changes and there are results
     */
    public static void validateGroupChange(WasteCategory category, WasteGroup previousGroup, boolean hasResults) {
        if (hasResults && previousGroup != category.getGroup()) {
            throw new BusinessRuleException("La subcategoría \"" + category.getName()
                    + "\" ya tiene resultados de cuarteo: no puede cambiar del grupo "
                    + (previousGroup == null ? "sin grupo" : previousGroup.getDisplayName())
                    + " a otro grupo.");
        }
    }

    /**
     * Validates that a subcategory can be deleted: one with quartering results
     * is only deactivated, never deleted (RN-042).
     *
     * @param category   subcategory to delete
     * @param hasResults whether the subcategory already has quartering results
     * @throws BusinessRuleException if the subcategory has results
     */
    public static void validateDeletion(WasteCategory category, boolean hasResults) {
        if (hasResults) {
            throw new BusinessRuleException("La subcategoría \"" + category.getName()
                    + "\" ya tiene resultados de cuarteo: no se puede eliminar, solo desactivar.");
        }
    }
}
