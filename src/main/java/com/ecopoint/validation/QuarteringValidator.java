package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.Permission;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.QualityStatus;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.WasteCategory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stateless validator for the quartering of a shift (RN-050 to RN-056),
 * applying GRASP Pure Fabrication and SOLID SRP. The 2 % quality control is
 * not a rejection: a non-conforming quartering is saved and raises an alert,
 * so it is evaluated by {@code Quartering.evaluateQuality}.
 *
 * @author craulgomez
 * @version 2.0
 */
public final class QuarteringValidator {

    private static final int WEIGHT_DECIMALS = 2;

    private QuarteringValidator() {
    }

    /**
     * Validates that the shift does not have a quartering yet (RN-050).
     *
     * @param shift shift that will receive the quartering
     * @throws BusinessRuleException if the shift already has one
     */
    public static void validateNoExistingQuartering(SamplingShift shift) {
        if (shift.hasQuartering()) {
            throw new BusinessRuleException("El turno " + shift.getCode() + " ya tiene un cuarteo registrado.");
        }
    }

    /**
     * Validates the sample weight and the subcategory weights (RN-051, RN-055).
     * When the shift generated less than the minimum sample, all its waste is
     * classified ("muestra completa"), so the sample must equal the shift net weight.
     *
     * @param quartering quartering to validate
     * @param settings   sample range of the institution
     * @throws InvalidDataException  if a weight is missing, negative or has too many decimals
     * @throws BusinessRuleException if the sample is outside the range or exceeds the shift weight
     */
    public static void validate(Quartering quartering, CharacterizationSettings settings) {
        if (quartering == null) {
            throw new InvalidDataException("cuarteo", "El cuarteo no puede ser nulo.");
        }
        BigDecimal sample = quartering.getSampleWeight();
        BigDecimal shiftNet = quartering.getShiftNetWeight();
        if (sample == null || sample.signum() <= 0) {
            throw new InvalidDataException("pesoSubmuestra", "El peso de la submuestra debe ser mayor que cero.");
        }
        requireDecimals(sample, "pesoSubmuestra");
        if (shiftNet == null || shiftNet.signum() <= 0) {
            throw new BusinessRuleException("El turno no tiene bolsas pesadas: no hay residuos para cuartear.");
        }
        if (sample.compareTo(shiftNet) > 0) {
            throw new BusinessRuleException("La submuestra (" + sample.toPlainString()
                    + " kg) no puede superar el peso neto del turno (" + shiftNet.toPlainString() + " kg).");
        }
        if (shiftNet.compareTo(settings.minSampleWeight()) < 0) {
            if (!quartering.isFullSample()) {
                throw new BusinessRuleException("El turno generó " + shiftNet.toPlainString()
                        + " kg, menos que la submuestra mínima: se debe clasificar todo (muestra completa de "
                        + shiftNet.toPlainString() + " kg).");
            }
        } else if (!settings.isSampleWeightInRange(sample)) {
            throw new BusinessRuleException("La submuestra debe estar entre "
                    + settings.minSampleWeight().toPlainString() + " y "
                    + settings.maxSampleWeight().toPlainString() + " kg.");
        }
        if (quartering.getResults().isEmpty()) {
            throw new InvalidDataException("resultados", "Registre el peso de al menos una subcategoría.");
        }
        for (QuarteringResult result : quartering.getResults()) {
            if (result.getWeight() == null || result.getWeight().signum() < 0) {
                throw new InvalidDataException("peso",
                        "El peso de " + result.getCategoryName() + " no puede ser vacío ni negativo.");
            }
            requireDecimals(result.getWeight(), "peso");
        }
    }

    /**
     * Validates that every result refers to an active subcategory of the
     * catalog and that no subcategory appears twice (RN-055).
     *
     * @param quartering quartering to validate
     * @param catalog    subcategories of the catalog, by identifier
     * @throws BusinessRuleException if a subcategory is unknown, inactive or repeated
     */
    public static void validateCategories(Quartering quartering, Map<Long, WasteCategory> catalog) {
        Set<Long> seen = new HashSet<>();
        for (QuarteringResult result : quartering.getResults()) {
            WasteCategory category = catalog == null ? null : catalog.get(result.getCategoryId());
            if (category == null || !category.isActive()) {
                throw new BusinessRuleException("La subcategoría " + result.getCategoryName()
                        + " no existe o está inactiva.");
            }
            if (!seen.add(result.getCategoryId())) {
                throw new BusinessRuleException("La subcategoría " + result.getCategoryName()
                        + " aparece dos veces en el cuarteo.");
            }
        }
    }

    /**
     * Validates that the user may register the sample: registering hazardous
     * waste (Sin color group) requires its own permission (RN-056, RN-081).
     *
     * @param quartering quartering being registered
     * @param user       user registering it
     * @throws com.ecopoint.exceptions.AccessDeniedException if the user lacks a required permission
     */
    public static void validatePermissions(Quartering quartering, User user) {
        AccessPolicy.require(user, Permission.REGISTER_QUARTERING);
        if (quartering.hasHazardousResults()) {
            AccessPolicy.require(user, Permission.REGISTER_HAZARDOUS_WASTE);
        }
    }

    /**
     * Validates that the user may annul the quartering: it must be
     * non-conforming and the user needs the permission (RN-052, RN-081).
     *
     * @param quartering quartering to annul
     * @param user       user annulling it
     * @throws BusinessRuleException if the quartering is not non-conforming
     * @throws com.ecopoint.exceptions.AccessDeniedException if the user lacks the permission
     */
    public static void validateAnnul(Quartering quartering, User user) {
        AccessPolicy.require(user, Permission.ANNUL_QUARTERING);
        if (quartering.getQualityStatus() != QualityStatus.NO_CONFORME) {
            throw new BusinessRuleException("Solo se puede anular un cuarteo no conforme.");
        }
    }

    private static void requireDecimals(BigDecimal value, String field) {
        if (value.stripTrailingZeros().scale() > WEIGHT_DECIMALS) {
            throw new InvalidDataException(field, "El peso admite como máximo " + WEIGHT_DECIMALS + " decimales.");
        }
    }
}
