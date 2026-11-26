package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.DensityMeasurement;
import com.ecopoint.model.SamplingShift;

import java.math.BigDecimal;

/**
 * Stateless validator for the density measurement of a shift (RN-057),
 * applying GRASP Pure Fabrication and SOLID SRP.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class DensityValidator {

    private static final int WEIGHT_DECIMALS = 2;
    private static final int VOLUME_DECIMALS = 3;
    static final String MIN_CONTAINER_VOLUME = "0.010";
    static final String MAX_CONTAINER_VOLUME = "1.000";
    static final String MIN_NET_WEIGHT = "0.1";
    static final String MAX_NET_WEIGHT = "500";

    private DensityValidator() {
    }

    /**
     * Validates the values of a density measurement: container volume and net
     * weight greater than zero, with 3 decimals in m³ and 2 in kg.
     *
     * @param measurement measurement to validate
     * @throws InvalidDataException if a value is missing, not positive or has too many decimals
     */
    public static void validate(DensityMeasurement measurement) {
        if (measurement == null) {
            throw new InvalidDataException("densidad", "La medición de densidad no puede ser nula.");
        }
        requirePositive(measurement.getContainerVolume(), "volumenRecipiente",
                "El volumen del recipiente", VOLUME_DECIMALS);
        requirePositive(measurement.getNetWeight(), "pesoNeto", "El peso neto", WEIGHT_DECIMALS);
        ValidationSupport.requireRange(measurement.getContainerVolume(), MIN_CONTAINER_VOLUME, MAX_CONTAINER_VOLUME,
                "volumenRecipiente", "El volumen del recipiente", "m³");
        ValidationSupport.requireRange(measurement.getNetWeight(), MIN_NET_WEIGHT, MAX_NET_WEIGHT,
                "pesoNeto", "El peso neto", "kg");
        ValidationSupport.requireRange(measurement.getDensity(), WasteBagValidator.MIN_DENSITY,
                WasteBagValidator.MAX_DENSITY, "pesoNeto", "La densidad", "kg/m³: revise el peso y el volumen");
    }

    /**
     * Validates that the shift does not have a density measurement yet (RN-057).
     *
     * @param shift shift that will receive the measurement
     * @throws BusinessRuleException if the shift already has one
     */
    public static void validateNoExistingDensity(SamplingShift shift) {
        ValidationSupport.requireNonNull(shift, "turno", "El turno no puede ser nulo.");
        if (shift.hasDensity()) {
            throw new BusinessRuleException("El turno " + shift.getCode() + " ya tiene una medición de densidad.");
        }
    }

    private static void requirePositive(BigDecimal value, String field, String label, int decimals) {
        if (value == null || value.signum() <= 0) {
            throw new InvalidDataException(field, label + " debe ser mayor que cero.");
        }
        if (value.stripTrailingZeros().scale() > decimals) {
            throw new InvalidDataException(field, label + " admite como máximo " + decimals + " decimales.");
        }
    }
}
