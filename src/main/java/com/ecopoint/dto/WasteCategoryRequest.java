package com.ecopoint.dto;

import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;

/**
 * Data to create or edit a waste subcategory (spec 004, R-324).
 *
 * @param name        name, unique ignoring case (RN-041)
 * @param group       group of Res. 2184 of 2019
 * @param description description
 * @param active      false to create it inactive (RN-043); ignored when editing
 * @author craulgomez
 * @version 1.0
 */
public record WasteCategoryRequest(String name, WasteGroup group, String description, Boolean active) {

    /**
     * Builds the subcategory of the domain, without id.
     *
     * @return the subcategory
     */
    public WasteCategory toModel() {
        WasteCategory category = new WasteCategory(null, name, group, description);
        if (Boolean.FALSE.equals(active)) {
            category.deactivate();
        }
        return category;
    }
}
