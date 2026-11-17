package com.ecopoint.dto;

import com.ecopoint.model.ShiftType;
import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Morning versus afternoon averages of the counted days (R-525).
 *
 * @param type              morning or afternoon
 * @param averageGeneration average generation of the shift in kg
 * @param averagePopulation average population of the shift
 * @param averagePpc        average PPC in kg/person/shift
 * @param averageDensity    average density in kg/m3
 * @param composition       composition of those shifts
 * @author craulgomez
 * @version 1.0
 */
public record ShiftComparisonDTO(ShiftType type, BigDecimal averageGeneration, BigDecimal averagePopulation,
                                 BigDecimal averagePpc, BigDecimal averageDensity, CompositionDTO composition) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static ShiftComparisonDTO from(StudyIndicators.ShiftTypeComparison value) {
        return new ShiftComparisonDTO(value.type(), Decimals.show(value.averageGeneration()),
                Decimals.show(value.averagePopulation()), Decimals.ppc(value.averagePpc()),
                Decimals.show(value.averageDensity()), CompositionDTO.from(value.composition()));
    }
}
