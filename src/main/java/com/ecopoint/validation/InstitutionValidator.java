package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Institution;

/**
 * Stateless validator for the basic data of an institution: name, address and
 * map zoom. Map center coordinates are validated by {@code GeoPoint} and the
 * settings by {@code CharacterizationSettings}.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class InstitutionValidator {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int MIN_ZOOM = 0;
    private static final int MAX_ZOOM = 22;

    private InstitutionValidator() {
    }

    /**
     * Validates the data of an institution.
     *
     * @param institution institution to validate
     * @throws InvalidDataException if a field is missing, too long or out of range
     */
    public static void validate(Institution institution) {
        if (institution == null) {
            throw new InvalidDataException("institucion", "La institución no puede ser nula.");
        }
        requireText(institution.getName(), "nombre", MAX_NAME_LENGTH);
        requireText(institution.getAddress(), "direccion", MAX_ADDRESS_LENGTH);
        if (institution.getMapZoom() < MIN_ZOOM || institution.getMapZoom() > MAX_ZOOM) {
            throw new InvalidDataException("zoom",
                    "El zoom del mapa debe estar entre " + MIN_ZOOM + " y " + MAX_ZOOM + ".");
        }
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(field, "El campo es obligatorio.");
        }
        if (value.length() > maxLength) {
            throw new InvalidDataException(field, "No puede superar " + maxLength + " caracteres.");
        }
    }
}
