package com.ecopoint.service;

import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Permission;
import com.ecopoint.model.User;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.persistence.WasteCategoryRepository;
import com.ecopoint.validation.AccessPolicy;
import com.ecopoint.validation.WasteCategoryValidator;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Catalog of waste subcategories of the four groups of Res. 2184 of 2019
 * (RN-040 to RN-042; spec 003, section 5.1; spec 004, R-324).
 *
 * @author EstefaniaMarquez
 * @version 1.0
 */
@Service
public class WasteCategoryService {

    private final WasteCategoryRepository categories;

    /**
     * Creates the service.
     *
     * @param categories subcategory repository
     */
    public WasteCategoryService(WasteCategoryRepository categories) {
        this.categories = categories;
    }

    /**
     * Lists the subcategories, optionally of one group.
     *
     * @param group group to keep, or null for all
     * @return subcategories
     */
    public List<WasteCategory> list(WasteGroup group) {
        return categories.findAll().stream()
                .filter(category -> group == null || category.getGroup() == group)
                .toList();
    }

    /**
     * Creates a subcategory with a unique name (RN-041).
     *
     * @param actor    teacher that acts
     * @param category data of the new subcategory
     * @return the saved subcategory
     */
    public WasteCategory create(User actor, WasteCategory category) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        category.setId(null);
        WasteCategoryValidator.validate(category);
        WasteCategoryValidator.validateUniqueName(category, categories.findNames());
        return categories.save(category);
    }

    /**
     * Updates a subcategory. One with quartering results cannot change its group (RN-042).
     *
     * @param actor   teacher that acts
     * @param id      subcategory id
     * @param changes new data
     * @return the saved subcategory
     */
    public WasteCategory update(User actor, Long id, WasteCategory changes) {
        AccessPolicy.require(actor, Permission.MANAGE_ADMINISTRATION);
        WasteCategory current = categories.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoría de residuo", id));
        Set<String> otherNames = new HashSet<>(categories.findNames());
        otherNames.removeIf(name -> WasteCategory.normalizeName(name)
                .equals(WasteCategory.normalizeName(current.getName())));
        WasteCategory updated = new WasteCategory(id, changes.getName(), changes.getGroup(), changes.getDescription());
        if (!current.isActive()) {
            updated.deactivate();
        }
        WasteCategoryValidator.validate(updated);
        WasteCategoryValidator.validateUniqueName(updated, otherNames);
        WasteCategoryValidator.validateGroupChange(updated, current.getGroup(), categories.hasResults(id));
        return categories.save(updated);
    }
}
