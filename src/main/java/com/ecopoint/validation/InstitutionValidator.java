package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.model.Institution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless validator for the basic data of an institution: name, an address
 * of Valledupar with its street nomenclature, map zoom and the logical ranges
 * of its settings (professor's review, points 1 and 2). Map center coordinates
 * are validated by {@code GeoPoint}.
 *
 * @author craulgomez
 * @version 1.0
 */
public final class InstitutionValidator {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int MIN_ZOOM = 0;
    private static final int MAX_ZOOM = 22;
    private static final int MAX_STREET_NUMBER = 150;
    private static final int MAX_PLATE_NUMBER = 199;
    private static final int MAX_KILOMETER = 30;
    private static final String CITY = ", Valledupar, Cesar";
    private static final String PLACE = "[A-Za-zÁÉÍÓÚÑÜáéíóúñü0-9 .]{1,60}";
    private static final Pattern URBAN_ADDRESS = Pattern.compile(
            "^(Calle|Carrera|Transversal|Diagonal|Avenida) (\\d{1,3})[A-H]?( Bis)? # (\\d{1,3})[A-H]? - (\\d{1,3})"
                    + "(, " + PLACE + "){0,2}" + Pattern.quote(CITY) + "$");
    private static final Pattern ROAD_ADDRESS = Pattern.compile(
            "^Kilómetro (\\d{1,2}) vía a " + PLACE + "(, " + PLACE + "){0,2}" + Pattern.quote(CITY) + "$");
    private static final String ADDRESS_FORMAT = "Escriba una dirección de Valledupar, por ejemplo "
            + "\"Calle 16A # 12B - 45, Novalito, Valledupar, Cesar\".";

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
        validateAddress(institution.getAddress().trim());
        if (institution.getMapZoom() < MIN_ZOOM || institution.getMapZoom() > MAX_ZOOM) {
            throw new InvalidDataException("zoom",
                    "El zoom del mapa debe estar entre " + MIN_ZOOM + " y " + MAX_ZOOM + ".");
        }
        if (institution.getSettings() != null) {
            validateSettings(institution.getSettings());
        }
    }

    /**
     * Validates that an address follows the street nomenclature of Valledupar:
     * "Calle 16A # 12B - 45" or "Kilómetro 5 vía a La Paz", followed by an
     * optional neighbourhood and detail and by the city.
     *
     * @param address address to check, already trimmed
     * @throws InvalidDataException if the format or a number is not logical
     */
    public static void validateAddress(String address) {
        Matcher urban = URBAN_ADDRESS.matcher(address);
        if (urban.matches()) {
            requireBetween(urban.group(2), MAX_STREET_NUMBER, "El número de la vía");
            requireBetween(urban.group(4), MAX_STREET_NUMBER, "El número después del #");
            requireBetween(urban.group(5), MAX_PLATE_NUMBER, "El número de la placa");
            return;
        }
        Matcher road = ROAD_ADDRESS.matcher(address);
        if (road.matches()) {
            requireBetween(road.group(1), MAX_KILOMETER, "El kilómetro");
            return;
        }
        throw new InvalidDataException("direccion", ADDRESS_FORMAT);
    }

    /**
     * Validates the logical ranges of the characterization settings; the order
     * of each minimum and maximum is checked by {@code CharacterizationSettings}.
     *
     * @param settings settings to check
     * @throws InvalidDataException if a value is outside its range
     */
    public static void validateSettings(CharacterizationSettings settings) {
        ValidationSupport.requireRange(settings.minSampleWeight(), "1", "200", "pesoMinimoSubmuestra",
                "El peso mínimo de la submuestra", "kg");
        ValidationSupport.requireRange(settings.maxSampleWeight(), "1", "200", "pesoMaximoSubmuestra",
                "El peso máximo de la submuestra", "kg");
        ValidationSupport.requireRange(settings.qualityTolerancePercent(), "0.5", "10", "toleranciaCalidad",
                "La tolerancia", "%");
        ValidationSupport.requireRange(settings.maxBagNetWeight(), "1", "100", "pesoMaximoBolsa",
                "El peso máximo de una bolsa", "kg");
        ValidationSupport.requireRange(settings.minBagDensity(), "1", "2000", "densidadMinimaBolsa",
                "La densidad mínima", "kg/m³");
        ValidationSupport.requireRange(settings.maxBagDensity(), "1", "2000", "densidadMaximaBolsa",
                "La densidad máxima", "kg/m³");
    }

    private static void requireBetween(String number, int max, String label) {
        int value = Integer.parseInt(number);
        if (value < 1 || value > max) {
            throw new InvalidDataException("direccion", label + " debe estar entre 1 y " + max + ".");
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
