package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.WasteBag;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Stateless validator for waste bags (RN-030 to RN-037) and the logical
 * ranges of their measures (professor's review, point 1), applying GRASP Pure
 * Fabrication and SOLID SRP. Shift conditions (open shift, population
 * registered) are checked by {@code ShiftValidator}.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class WasteBagValidator {

    private static final int WEIGHT_DECIMALS = 2;
    private static final int VOLUME_DECIMALS = 3;
    static final String MIN_GROSS_WEIGHT = "0.01";
    static final String MAX_GROSS_WEIGHT = "60";
    static final String MAX_TARE = "5";
    static final String MIN_VOLUME = "0.005";
    static final String MAX_VOLUME = "0.300";
    static final String MIN_DENSITY = "10";
    static final String MAX_DENSITY = "1500";

    private WasteBagValidator() {
    }

    /**
     * Validates the data of a bag: participating source, weights and volume.
     *
     * @param bag                    bag to validate
     * @param participatingSourceIds sources taking part in the study
     * @throws InvalidDataException  if a value is missing, negative, has too many decimals
     *                               or the gross weight does not exceed the tare
     * @throws BusinessRuleException if the source does not take part in the study
     */
    public static void validate(WasteBag bag, Set<Long> participatingSourceIds) {
        if (bag == null) {
            throw new InvalidDataException("bolsa", "La bolsa no puede ser nula.");
        }
        if (bag.getSourceId() == null) {
            throw new InvalidDataException("fuente", "Debe elegir la fuente de la que proviene la bolsa.");
        }
        if (participatingSourceIds != null && !participatingSourceIds.contains(bag.getSourceId())) {
            throw new BusinessRuleException("La fuente " + bag.getSourceCode()
                    + " no participa en este estudio.");
        }
        requirePositive(bag.getGrossWeight(), "pesoBruto", "El peso bruto", WEIGHT_DECIMALS);
        ValidationSupport.requireRange(bag.getGrossWeight(), MIN_GROSS_WEIGHT, MAX_GROSS_WEIGHT,
                "pesoBruto", "El peso bruto", "kg");
        if (bag.getTare() == null) {
            throw new InvalidDataException("tara", "La tara es obligatoria (0 si se pesó sin recipiente).");
        }
        if (bag.getTare().signum() < 0) {
            throw new InvalidDataException("tara", "La tara no puede ser negativa.");
        }
        requireDecimals(bag.getTare(), "tara", "La tara", WEIGHT_DECIMALS);
        ValidationSupport.requireRange(bag.getTare(), "0", MAX_TARE, "tara", "La tara", "kg");
        if (bag.getGrossWeight().compareTo(bag.getTare()) <= 0) {
            throw new InvalidDataException("tara", "La tara no puede ser mayor o igual que el peso bruto.");
        }
        requirePositive(bag.getVolume(), "volumen", "El volumen", VOLUME_DECIMALS);
        ValidationSupport.requireRange(bag.getVolume(), MIN_VOLUME, MAX_VOLUME, "volumen",
                "El volumen de la bolsa", "m³");
        ValidationSupport.requireRange(bag.getDensity(), MIN_DENSITY, MAX_DENSITY, "volumen",
                "La densidad de la bolsa", "kg/m³: revise el peso y el volumen");
    }

    /**
     * Indicates whether a bag is outside the plausibility limits of the
     * institution (RN-037). Atypical bags are accepted but raise an alert.
     *
     * @param bag      bag to check
     * @param settings plausibility limits of the institution
     * @return true if the net weight or the density is not plausible
     */
    public static boolean isAtypical(WasteBag bag, CharacterizationSettings settings) {
        ValidationSupport.requireNonNull(bag, "bolsa", "La bolsa no puede ser nula.");
        return !settings.isBagNetWeightPlausible(bag.getNetWeight())
                || !settings.isBagDensityPlausible(bag.getDensity());
    }

    private static void requirePositive(BigDecimal value, String field, String label, int decimals) {
        if (value == null) {
            throw new InvalidDataException(field, label + " es obligatorio.");
        }
        if (value.signum() <= 0) {
            throw new InvalidDataException(field, label + " debe ser mayor que cero.");
        }
        requireDecimals(value, field, label, decimals);
    }

    private static void requireDecimals(BigDecimal value, String field, String label, int decimals) {
        if (value.stripTrailingZeros().scale() > decimals) {
            throw new InvalidDataException(field, label + " admite como máximo " + decimals + " decimales.");
        }
    }
}
