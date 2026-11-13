package com.ecopoint.dto;

import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Average daily generation of one source (R-524).
 *
 * @param sourceId               source id
 * @param code                   source code
 * @param name                   source name
 * @param averageDailyGeneration average daily generation in kg/day
 * @author craulgomez
 * @version 1.0
 */
public record SourceGenerationDTO(Long sourceId, String code, String name, BigDecimal averageDailyGeneration) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static SourceGenerationDTO from(StudyIndicators.SourceGeneration value) {
        return new SourceGenerationDTO(value.sourceId(), value.code(), value.name(),
                Decimals.show(value.averageDailyGeneration()));
    }
}
