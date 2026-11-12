package com.ecopoint.dto;

import com.ecopoint.model.CharacterizationSettings;

import java.math.BigDecimal;

/**
 * Characterization settings of an institution: subsample weight, quality
 * tolerance and plausibility limits of the bags (RN-037, RN-051, RN-052).
 *
 * @param minSampleWeight         minimum subsample weight, in kg
 * @param maxSampleWeight         maximum subsample weight, in kg
 * @param qualityTolerancePercent tolerance of the quality control, in %
 * @param maxBagNetWeight         largest plausible net weight of a bag, in kg
 * @param minBagDensity           smallest plausible density of a bag, in kg/m3
 * @param maxBagDensity           largest plausible density of a bag, in kg/m3
 * @author craulgomez
 * @version 1.0
 */
public record SettingsDto(BigDecimal minSampleWeight, BigDecimal maxSampleWeight, BigDecimal qualityTolerancePercent,
                          BigDecimal maxBagNetWeight, BigDecimal minBagDensity, BigDecimal maxBagDensity) {

    /**
     * Copies the settings of the domain.
     *
     * @param settings domain settings
     * @return the DTO
     */
    public static SettingsDto from(CharacterizationSettings settings) {
        return new SettingsDto(settings.minSampleWeight(), settings.maxSampleWeight(),
                settings.qualityTolerancePercent(), settings.maxBagNetWeight(), settings.minBagDensity(),
                settings.maxBagDensity());
    }

    /**
     * Builds the domain settings, which validate their ranges.
     *
     * @return the domain settings
     */
    public CharacterizationSettings toModel() {
        return new CharacterizationSettings(minSampleWeight, maxSampleWeight, qualityTolerancePercent,
                maxBagNetWeight, minBagDensity, maxBagDensity);
    }
}
