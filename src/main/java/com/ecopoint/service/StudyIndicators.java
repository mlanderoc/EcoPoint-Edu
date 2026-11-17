package com.ecopoint.service;

import com.ecopoint.model.ShiftType;
import com.ecopoint.model.StudyStatus;
import com.ecopoint.model.WasteGroup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Baseline indicators of a study with full precision (spec 006, section 5.2).
 * Values are rounded only when they are shown or exported (R-511); a value is
 * {@code null} when the data needed to calculate it is missing (R-512).
 *
 * @param studyId     study id
 * @param status      study status
 * @param official    whether the study is closed and the values are official (R-502)
 * @param summary     main indicators
 * @param days        generation and population of days 1 to 8
 * @param sources     average daily generation by source, highest first
 * @param shiftTypes  morning versus afternoon comparison
 * @param composition total physical composition
 * @param quality     data quality of the study
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record StudyIndicators(Long studyId, StudyStatus status, boolean official, Summary summary,
                              List<DailyGeneration> days, List<SourceGeneration> sources,
                              List<ShiftTypeComparison> shiftTypes, Composition composition, DataQuality quality) {

    /**
     * Main indicators of the baseline (R-521).
     *
     * @param averageDailyGeneration     Ḡ in kg/day
     * @param averagePpc                 average PPC in kg/person/day
     * @param ppcStandardDeviation       sample standard deviation of the daily PPC
     * @param ppcVariationCoefficient    CV of the PPC in percent
     * @param averageDensity             average of the shift densities in kg/m³
     * @param recoverableFraction        BLANCO plus VERDE composition in percent
     * @param recoverableDailyGeneration estimated recoverable generation in kg/day
     * @param countedDays                days that enter the calculation (2 to 8)
     */
    public record Summary(BigDecimal averageDailyGeneration, BigDecimal averagePpc, BigDecimal ppcStandardDeviation,
                          BigDecimal ppcVariationCoefficient, BigDecimal averageDensity,
                          BigDecimal recoverableFraction, BigDecimal recoverableDailyGeneration, int countedDays) {
    }

    /**
     * Generation and population of one sampling day.
     *
     * @param dayNumber           day 1 to 8
     * @param date                scheduled date
     * @param excluded            whether the day stays out of the calculations (day 1)
     * @param morningGeneration   G(d,M) in kg
     * @param afternoonGeneration G(d,T) in kg
     * @param generation          G(d) in kg
     * @param population          P(d) in persons
     * @param ppc                 PPC(d), or null without population
     */
    public record DailyGeneration(int dayNumber, LocalDate date, boolean excluded, BigDecimal morningGeneration,
                                  BigDecimal afternoonGeneration, BigDecimal generation, int population,
                                  BigDecimal ppc) {
    }

    /**
     * Average daily generation of one source.
     *
     * @param sourceId               source id
     * @param code                   source code
     * @param name                   source name
     * @param averageDailyGeneration Ḡ of the source in kg/day
     */
    public record SourceGeneration(Long sourceId, String code, String name, BigDecimal averageDailyGeneration) {
    }

    /**
     * Averages of the morning or afternoon shifts of the counted days (R-525).
     *
     * @param type                     morning or afternoon
     * @param averageGeneration        average G(d,t) in kg
     * @param averagePopulation        average P(d,t) in persons
     * @param averagePpc               average PPC(d,t) in kg/person/shift
     * @param averageDensity           average density of those shifts in kg/m³
     * @param composition              composition of those shifts
     */
    public record ShiftTypeComparison(ShiftType type, BigDecimal averageGeneration, BigDecimal averagePopulation,
                                      BigDecimal averagePpc, BigDecimal averageDensity, Composition composition) {
    }

    /**
     * Physical composition weighted by mass (R-510).
     *
     * @param groups               composition of the four groups
     * @param subcategories        composition of each subcategory, highest first
     * @param shiftsWithQuartering shifts whose valid quartering entered (R-512)
     * @param expectedShifts       shifts of the counted days
     */
    public record Composition(List<GroupShare> groups, List<SubcategoryShare> subcategories,
                              int shiftsWithQuartering, int expectedShifts) {
    }

    /**
     * Share of one group of Resolution 2184 of 2019.
     *
     * @param group      group
     * @param weight     classified weight in kg
     * @param percentage share in percent, or null without quarterings
     */
    public record GroupShare(WasteGroup group, BigDecimal weight, BigDecimal percentage) {
    }

    /**
     * Share of one subcategory.
     *
     * @param categoryId category id
     * @param name       category name
     * @param group      its group
     * @param weight     classified weight in kg
     * @param percentage share in percent
     */
    public record SubcategoryShare(Long categoryId, String name, WasteGroup group, BigDecimal weight,
                                   BigDecimal percentage) {
    }

    /**
     * Data quality of the study (R-526).
     *
     * @param totalRecords             verifiable records
     * @param verifiedRecords          records verified or corrected
     * @param verifiedPercentage       verified share in percent
     * @param correctedRecords         records corrected against the paper form
     * @param nonConformingQuarterings non-conforming quarterings
     */
    public record DataQuality(int totalRecords, int verifiedRecords, BigDecimal verifiedPercentage,
                              int correctedRecords, int nonConformingQuarterings) {
    }
}
