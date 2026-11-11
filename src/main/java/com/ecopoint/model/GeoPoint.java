package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable geographic point in WGS84 coordinates (latitude and longitude in
 * decimal degrees). Coordinates are normalized to 7 decimal places (about 1 cm),
 * so two points with the same position are always equal (RN-004).
 *
 * @param latitude  latitude between -90 and 90
 * @param longitude longitude between -180 and 180
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record GeoPoint(BigDecimal latitude, BigDecimal longitude) {

    private static final int SCALE = 7;
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    /**
     * Validates the ranges and normalizes the scale of both coordinates.
     *
     * @throws InvalidDataException if a coordinate is missing or out of range
     */
    public GeoPoint {
        latitude = normalize(latitude, MAX_LATITUDE, "latitud", "-90 y 90");
        longitude = normalize(longitude, MAX_LONGITUDE, "longitud", "-180 y 180");
    }

    private static BigDecimal normalize(BigDecimal value, BigDecimal limit, String field, String range) {
        if (value == null) {
            throw new InvalidDataException(field, "La coordenada es obligatoria.");
        }
        if (value.abs().compareTo(limit) > 0) {
            throw new InvalidDataException(field,
                    "El valor " + value.toPlainString() + " está fuera del rango permitido (entre " + range + ").");
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Creates a point from text values, convenient for tests and data import.
     *
     * @param latitude  latitude as text, e.g. "10.4631"
     * @param longitude longitude as text, e.g. "-73.2532"
     * @return the validated point
     */
    public static GeoPoint of(String latitude, String longitude) {
        return new GeoPoint(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
