package com.ecopoint.dto;

import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;

/**
 * Main indicators of the baseline (spec 006, R-521), with 2 decimals and PPC with 3.
 *
 * @param averageDailyGeneration     average daily generation in kg/day
 * @param averagePpc                 average PPC in kg/person/day
 * @param ppcStandardDeviation       sample standard deviation of the PPC
 * @param ppcVariationCoefficient    CV of the PPC in percent
 * @param averageDensity             average density in kg/m3
 * @param recoverableFraction        recoverable fraction in percent
 * @param recoverableDailyGeneration estimated recoverable generation in kg/day
 * @param countedDays                days that enter the calculation
 * @author craulgomez
 * @version 1.0
 */
public record SummaryKPIsDTO(BigDecimal averageDailyGeneration, BigDecimal averagePpc, BigDecimal ppcStandardDeviation,
                             BigDecimal ppcVariationCoefficient, BigDecimal averageDensity,
                             BigDecimal recoverableFraction, BigDecimal recoverableDailyGeneration, int countedDays) {

    /**
     * Rounds the values calculated by the indicator service (R-511).
     *
     * @param value value with full precision
     * @return the DTO
     */
    public static SummaryKPIsDTO from(StudyIndicators.Summary value) {
        return new SummaryKPIsDTO(Decimals.show(value.averageDailyGeneration()), Decimals.ppc(value.averagePpc()),
                Decimals.ppc(value.ppcStandardDeviation()), Decimals.show(value.ppcVariationCoefficient()),
                Decimals.show(value.averageDensity()), Decimals.show(value.recoverableFraction()),
                Decimals.show(value.recoverableDailyGeneration()), value.countedDays());
    }
}
