package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;

/**
 * Small helpers shared by the validators, so a missing object is always
 * reported as an {@link InvalidDataException} and never as a
 * NullPointerException (AGENTS.md section 5).
 *
 * @author craulgomez
 * @version 1.0
 */
final class ValidationSupport {

    private ValidationSupport() {
    }

    /**
     * Requires that a value is present.
     *
     * @param value   value to check
     * @param field   field name reported in the exception
     * @param message message in Spanish for the user
     * @throws InvalidDataException if the value is null
     */
    static void requireNonNull(Object value, String field, String message) {
        if (value == null) {
            throw new InvalidDataException(field, message);
        }
    }
}
