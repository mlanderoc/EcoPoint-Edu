package com.ecopoint.validation;

import com.ecopoint.exceptions.BusinessRuleException;
import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.GenerationSource;

import java.util.Set;

/**
 * Stateless validator for generation sources (RN-001 to RN-005), applying
 * GRASP Pure Fabrication and SOLID SRP. Rules that need data from other
 * sources receive it as parameters, because the domain does not query the
 * database.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class GenerationSourceValidator {

    private static final int MAX_NAME_LENGTH = 80;

    private GenerationSourceValidator() {
    }

    /**
     * Validates the data of a source: institution, code format, name and type.
     * The polygon, when present, is already validated by {@code GeoPolygon} (RN-004).
     *
     * @param source source to validate
     * @throws InvalidDataException if a field is missing or has an invalid format
     */
    public static void validate(GenerationSource source) {
        if (source == null) {
            throw new InvalidDataException("fuente", "La fuente de generación no puede ser nula.");
        }
        if (source.getInstitutionId() == null) {
            throw new InvalidDataException("institucion", "La fuente debe pertenecer a una institución.");
        }
        if (!GenerationSource.isValidCode(source.getCode())) {
            throw new InvalidDataException("codigo",
                    "El código debe tener de 2 a 6 caracteres, solo letras mayúsculas y dígitos (ej. AU1, PAE).");
        }
        if (source.getName() == null || source.getName().isBlank()) {
            throw new InvalidDataException("nombre", "El nombre de la fuente es obligatorio.");
        }
        if (source.getName().length() > MAX_NAME_LENGTH) {
            throw new InvalidDataException("nombre",
                    "El nombre de la fuente no puede superar " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (source.getType() == null) {
            throw new InvalidDataException("tipo", "El tipo de fuente es obligatorio.");
        }
    }

    /**
     * Validates that the code is not already used by another source of the
     * same institution (RN-002). When editing a source, the caller must exclude
     * the source's own current code from the set.
     *
     * @param source        source being created or edited
     * @param existingCodes codes of the other sources of the same institution
     * @throws BusinessRuleException if the code is already taken
     */
    public static void validateUniqueCode(GenerationSource source, Set<String> existingCodes) {
        if (existingCodes != null && existingCodes.contains(source.getCode())) {
            throw new BusinessRuleException("Ya existe una fuente con el código " + source.getCode()
                    + " en esta institución.");
        }
    }

    /**
     * Validates that the source can be deleted: a source with bags is only
     * deactivated, never deleted (RN-005).
     *
     * @param source  source to delete
     * @param hasBags whether the source already has registered bags
     * @throws BusinessRuleException if the source has bags
     */
    public static void validateDeletion(GenerationSource source, boolean hasBags) {
        if (hasBags) {
            throw new BusinessRuleException("La fuente " + source.getCode()
                    + " tiene bolsas registradas: no se puede eliminar, solo desactivar.");
        }
    }

    /**
     * Validates that the source may receive new field records (RN-005).
     *
     * @param source source chosen for a new record
     * @throws BusinessRuleException if the source is inactive
     */
    public static void validateAvailableForRegistration(GenerationSource source) {
        if (!source.isAvailableForRegistration()) {
            throw new BusinessRuleException("La fuente " + source.getCode()
                    + " está inactiva y no admite nuevos registros.");
        }
    }
}
