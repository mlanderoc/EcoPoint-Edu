package com.ecopoint.dto;

import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;

/**
 * Data to create or edit a waste subcategory (spec 004, R-324).
 *
 * @param name        name, unique ignoring case (RN-041)
 * @param group       group of Res. 2184 of 2019
 * @param description description
 * @author craulgomez
 * @version 1.0
 */
public record WasteCategoryRequest(String name, WasteGroup group, String description) {

    /**
     * Builds the subcategory of the domain, without id.
     *
     * @return the subcategory
     */
    public WasteCategory toModel() {
        return new WasteCategory(null, name, group, description);
    }
}
