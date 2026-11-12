package com.ecopoint.dto;

import com.ecopoint.model.GeoPoint;

import java.math.BigDecimal;

/**
 * Point with latitude and longitude in WGS84 (spec 003; spec 007, R-600).
 *
 * @param latitude  latitude, between -90 and 90
 * @param longitude longitude, between -180 and 180
 * @author craulgomez
 * @version 1.0
 */
public record GeoPointDto(BigDecimal latitude, BigDecimal longitude) {

    /**
     * Copies a point of the domain.
     *
     * @param point domain point, or null
     * @return the DTO, or null
     */
    public static GeoPointDto from(GeoPoint point) {
        return point == null ? null : new GeoPointDto(point.latitude(), point.longitude());
    }

    /**
     * Builds the domain point, which validates the ranges (RN-004).
     *
     * @return the domain point
     */
    public GeoPoint toModel() {
        return new GeoPoint(latitude, longitude);
    }
}
