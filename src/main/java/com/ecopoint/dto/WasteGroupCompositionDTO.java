package com.ecopoint.dto;

import com.ecopoint.model.WasteGroup;
import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Share of one group of Resolution 2184 of 2019 (R-523).
 *
 * @param group       group
 * @param name        color name, e.g. Blanco
 * @param description group description
 * @param weight      classified weight in kg
 * @param percentage  share in percent, or null without quarterings
 * @author craulgomez
 * @version 1.0
 */
public record WasteGroupCompositionDTO(WasteGroup group, String name, String description, BigDecimal weight,
                                       BigDecimal percentage) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static WasteGroupCompositionDTO from(StudyIndicators.GroupShare value) {
        return new WasteGroupCompositionDTO(value.group(), value.group().getDisplayName(),
                value.group().getDescription(), Decimals.show(value.weight()), Decimals.show(value.percentage()));
    }
}
