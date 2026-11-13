package com.ecopoint.dto;

import com.ecopoint.model.WasteGroup;
import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Share of one subcategory (R-523).
 *
 * @param categoryId category id
 * @param name       category name
 * @param group      its group
 * @param weight     classified weight in kg
 * @param percentage share in percent
 * @author craulgomez
 * @version 1.0
 */
public record WasteSubcategoryCompositionDTO(Long categoryId, String name, WasteGroup group, BigDecimal weight,
                                             BigDecimal percentage) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static WasteSubcategoryCompositionDTO from(StudyIndicators.SubcategoryShare value) {
        return new WasteSubcategoryCompositionDTO(value.categoryId(), value.name(), value.group(),
                Decimals.show(value.weight()), Decimals.show(value.percentage()));
    }
}
