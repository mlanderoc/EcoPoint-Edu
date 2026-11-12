package com.ecopoint.dto;

/**
 * Invalid field of a request, reported inside {@link ApiError} (spec 003, R-110).
 *
 * @param field   name of the JSON field, in camelCase
 * @param message reason in Spanish
 * @author craulgomez
 * @version 1.0
 */
public record FieldErrorDto(String field, String message) {
}
