package com.ecopoint.dto;

import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;

/**
 * Waste subcategory as the API returns it.
 *
 * @param id          subcategory id
 * @param name        name
 * @param group       group of Res. 2184 of 2019
 * @param description description
 * @param active      whether it can be used in new quarterings
 * @param hazardous   whether it belongs to the SIN_COLOR group (RN-056)
 * @author craulgomez
 * @version 1.0
 */
public record WasteCategoryResponse(Long id, String name, WasteGroup group, String description, boolean active,
                                    boolean hazardous) {

    /**
     * Copies a subcategory of the domain.
     *
     * @param category domain subcategory
     * @return the response
     */
    public static WasteCategoryResponse from(WasteCategory category) {
        return new WasteCategoryResponse(category.getId(), category.getName(), category.getGroup(),
                category.getDescription(), category.isActive(), category.isHazardous());
    }
}
