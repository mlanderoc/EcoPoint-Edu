package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.exceptions.ResourceNotFoundException;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stateless validator for the solid waste Quartering Method ({@link Quartering}
 * and {@link QuarteringResult}) enforcing physical weight coherence and domain rules
 * (GRASP Pure Fabrication &amp; SOLID SRP).
 *
 * @author Carlos Raul Gomez Galvis
 * @version 1.0
 */
public final class QuarteringValidator {

    private QuarteringValidator() {
    }

    /**
     * Validates all inputs and physical weight rules of a {@link Quartering} procedure.
     *
     * @param quartering quartering procedure to validate
     * @throws InvalidDataException  if weights or quantities are null or negative
     * @throws BusinessRuleException if sample weight exceeds initial weight or classified weights exceed sample weight
     */
    public static void validateQuarteringData(Quartering quartering) {
        if (quartering == null) {
            throw new InvalidDataException("cuarteo", "El registro de cuarteo no puede ser nulo.");
        }
        if (quartering.getIdRecord() <= 0) {
            throw new InvalidDataException("idRegistroJornada",
                    "El identificador del registro de inspección asociado debe ser mayor que cero.");
        }
        if (quartering.getInitialWeight() == null
                || quartering.getInitialWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("pesoInicial",
                    "El peso inicial recolectado debe ser mayor que 0.00 kg.");
        }
        if (quartering.getSampleWeight() == null
                || quartering.getSampleWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidDataException("pesoMuestra",
                    "El peso de la muestra de cuarteo debe ser mayor que 0.00 kg.");
        }

        List<QuarteringResult> results = quartering.getResults();
        if (results == null || results.isEmpty()) {
            throw new InvalidDataException("resultados",
                    "El procedimiento de cuarteo debe incluir al menos un resultado de clasificación.");
        }

        for (QuarteringResult result : results) {
            if (result == null) {
                throw new InvalidDataException("resultadoCuarteo",
                        "Ningún renglón de resultado de cuarteo puede ser nulo.");
            }
            if (result.getIdWaste() <= 0) {
                throw new InvalidDataException("idResiduo",
                        "El identificador de la categoría de residuo debe ser mayor que cero.");
            }
            if (result.getWeight() == null || result.getWeight().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidDataException("peso",
                        "El peso clasificado por categoría no puede ser negativo.");
            }
            if (result.getQuantity() < 0) {
                throw new InvalidDataException("cantidad",
                        "La cantidad de unidades por categoría no puede ser negativa.");
            }
        }

        validateWeightCoherence(quartering);
    }

    /**
     * Enforces physical weight coherence rules for the Quartering Method:
     * <ol>
     *   <li>{@code sampleWeight <= initialWeight}</li>
     *   <li>{@code sum(result.weight) <= sampleWeight}</li>
     * </ol>
     *
     * @param quartering quartering instance to verify
     * @throws BusinessRuleException if physical weight constraints are violated
     */
    public static void validateWeightCoherence(Quartering quartering) {
        BigDecimal initialWeight = quartering.getInitialWeight();
        BigDecimal sampleWeight = quartering.getSampleWeight();

        if (sampleWeight.compareTo(initialWeight) > 0) {
            throw new BusinessRuleException("El peso de la muestra (" + sampleWeight
                    + " kg) no puede ser mayor que el peso inicial recolectado (" + initialWeight + " kg).");
        }

        BigDecimal totalClassified = quartering.calculateTotalClassifiedWeight();
        if (totalClassified.compareTo(sampleWeight) > 0) {
            throw new BusinessRuleException("La suma de los pesos clasificados (" + totalClassified
                    + " kg) excede el peso total de la muestra de cuarteo (" + sampleWeight + " kg).");
        }
    }

    /**
     * Verifies that an inspection record does not already have a registered quartering
     * procedure (enforcing the 1 to 0..1 relationship).
     *
     * @param idRecord            inspection record identifier
     * @param existingQuarterings list of registered quartering procedures
     * @throws BusinessRuleException if a quartering already exists for the inspection record
     */
    public static void validateUniqueQuarteringPerRecord(int idRecord, List<Quartering> existingQuarterings) {
        if (existingQuarterings == null) {
            return;
        }
        for (Quartering existing : existingQuarterings) {
            if (existing != null && existing.getIdRecord() == idRecord) {
                throw new BusinessRuleException(
                        "El registro de inspección con identificador '" + idRecord
                                + "' ya tiene un procedimiento de cuarteo registrado.");
            }
        }
    }

    /**
     * Verifies that a {@link Quartering} instance exists.
     *
     * @param quartering   quartering reference
     * @param idQuartering searched identifier
     * @throws ResourceNotFoundException if {@code quartering} is {@code null}
     */
    public static void validateQuarteringExists(Quartering quartering, int idQuartering) {
        if (quartering == null) {
            throw new ResourceNotFoundException("Cuarteo", idQuartering);
        }
    }
}