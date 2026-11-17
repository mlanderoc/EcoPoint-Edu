package com.ecopoint.dto;

import com.ecopoint.service.StudyIndicators;

import java.util.List;

/**
 * Physical composition weighted by mass (R-510, R-512, R-523).
 *
 * @param groups               shares of the four groups
 * @param subcategories        shares of the subcategories, highest first
 * @param shiftsWithQuartering shifts whose valid quartering entered
 * @param expectedShifts       shifts of the counted days
 * @param note                 note on the shifts used (CA-505)
 * @author craulgomez
 * @version 1.0
 */
public record CompositionDTO(List<WasteGroupCompositionDTO> groups, List<WasteSubcategoryCompositionDTO> subcategories,
                             int shiftsWithQuartering, int expectedShifts, String note) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static CompositionDTO from(StudyIndicators.Composition value) {
        return new CompositionDTO(value.groups().stream().map(WasteGroupCompositionDTO::from).toList(),
                value.subcategories().stream().map(WasteSubcategoryCompositionDTO::from).toList(),
                value.shiftsWithQuartering(), value.expectedShifts(),
                "Composición calculada con " + value.shiftsWithQuartering() + " de " + value.expectedShifts() + " turnos");
    }
}
