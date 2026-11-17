package com.ecopoint.dto;

import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Generation and population of one sampling day (R-522).
 *
 * @param dayNumber           day 1 to 8
 * @param date                scheduled date
 * @param excluded            whether it is the discard day
 * @param morningGeneration   morning generation in kg
 * @param afternoonGeneration afternoon generation in kg
 * @param generation          daily generation in kg
 * @param population          population of the day
 * @param ppc                 PPC of the day, or null without population
 * @author craulgomez
 * @version 1.0
 */
public record DailyGenerationDTO(int dayNumber, LocalDate date, boolean excluded, BigDecimal morningGeneration,
                                 BigDecimal afternoonGeneration, BigDecimal generation, int population, BigDecimal ppc) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static DailyGenerationDTO from(StudyIndicators.DailyGeneration value) {
        return new DailyGenerationDTO(value.dayNumber(), value.date(), value.excluded(),
                Decimals.show(value.morningGeneration()), Decimals.show(value.afternoonGeneration()),
                Decimals.show(value.generation()), value.population(), Decimals.ppc(value.ppc()));
    }
}
