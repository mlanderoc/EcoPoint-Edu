package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.math.BigDecimal;

/**
 * Immutable, configurable parameters of a characterization study for one
 * institution: quartering sample range (D8, RN-051), quality control tolerance
 * (RN-052) and plausibility limits for bags (RN-037).
 *
 * @param minSampleWeight         minimum quartering sample weight in kg
 * @param maxSampleWeight         maximum quartering sample weight in kg
 * @param qualityTolerancePercent maximum allowed difference between the sample
 *                                and the sum of subcategories, in percent
 * @param maxBagNetWeight         plausible maximum net weight of a bag in kg
 * @param minBagDensity           plausible minimum bag density in kg/m³
 * @param maxBagDensity           plausible maximum bag density in kg/m³
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record CharacterizationSettings(BigDecimal minSampleWeight,
                                       BigDecimal maxSampleWeight,
                                       BigDecimal qualityTolerancePercent,
                                       BigDecimal maxBagNetWeight,
                                       BigDecimal minBagDensity,
                                       BigDecimal maxBagDensity) {

    /**
     * Validates that every value is present and positive, and that each
     * minimum is lower than its maximum.
     *
     * @throws InvalidDataException if a value is missing, not positive or inconsistent
     */
    public CharacterizationSettings {
        requirePositive(minSampleWeight, "pesoMinimoSubmuestra");
        requirePositive(maxSampleWeight, "pesoMaximoSubmuestra");
        requirePositive(qualityTolerancePercent, "toleranciaCalidad");
        requirePositive(maxBagNetWeight, "pesoMaximoBolsa");
        requirePositive(minBagDensity, "densidadMinimaBolsa");
        requirePositive(maxBagDensity, "densidadMaximaBolsa");
        if (minSampleWeight.compareTo(maxSampleWeight) >= 0) {
            throw new InvalidDataException("pesoMinimoSubmuestra",
                    "El peso mínimo de la submuestra debe ser menor que el máximo.");
        }
        if (minBagDensity.compareTo(maxBagDensity) >= 0) {
            throw new InvalidDataException("densidadMinimaBolsa",
                    "La densidad mínima debe ser menor que la máxima.");
        }
    }

    /**
     * Returns the default settings of the specs: sample of 45 to 55 kg,
     * 2 % tolerance, bags up to 50 kg and densities between 20 and 1 000 kg/m³.
     *
     * @return default settings
     */
    public static CharacterizationSettings defaults() {
        return new CharacterizationSettings(
                new BigDecimal("45"), new BigDecimal("55"), new BigDecimal("2"),
                new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("1000"));
    }

    /**
     * Indicates whether a sample weight lies inside the configured range (inclusive).
     *
     * @param sampleWeight sample weight in kg
     * @return true if min &lt;= weight &lt;= max
     */
    public boolean isSampleWeightInRange(BigDecimal sampleWeight) {
        return isBetween(sampleWeight, minSampleWeight, maxSampleWeight);
    }

    /**
     * Indicates whether a bag net weight is plausible (RN-037).
     *
     * @param netWeight bag net weight in kg
     * @return true if the weight does not exceed the configured maximum
     */
    public boolean isBagNetWeightPlausible(BigDecimal netWeight) {
        return netWeight != null && netWeight.compareTo(maxBagNetWeight) <= 0;
    }

    /**
     * Indicates whether a bag density is plausible (RN-037).
     *
     * @param density bag density in kg/m³
     * @return true if min &lt;= density &lt;= max
     */
    public boolean isBagDensityPlausible(BigDecimal density) {
        return isBetween(density, minBagDensity, maxBagDensity);
    }

    private static boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidDataException(field, "El valor debe ser mayor que cero.");
        }
    }
}
