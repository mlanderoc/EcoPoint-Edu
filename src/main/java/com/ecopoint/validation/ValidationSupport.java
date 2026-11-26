package com.ecopoint.validation;

import com.ecopoint.exceptions.InvalidDataException;

import java.math.BigDecimal;

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

    /**
     * Requires that a measured value lies inside its logical range, so absurd
     * values are rejected before saving (professor's review, point 1).
     *
     * @param value value to check, already known not to be null
     * @param min   lowest allowed value, inclusive
     * @param max   highest allowed value, inclusive
     * @param field field name reported in the exception
     * @param label name of the value for the user, starting with an article
     * @param unit  unit shown after the numbers, such as "kg"
     * @throws InvalidDataException if the value is outside the range
     */
    static void requireRange(BigDecimal value, String min, String max, String field, String label, String unit) {
        if (value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0) {
            throw new InvalidDataException(field, label + " debe estar entre " + display(min) + " y "
                    + display(max) + " " + unit + ".");
        }
    }

    /**
     * Requires that a count lies inside its logical range.
     *
     * @param value count to check
     * @param max   highest allowed count, inclusive; the lowest is zero
     * @param field field name reported in the exception
     * @param label name of the count for the user, starting with an article
     * @throws InvalidDataException if the count is negative or above the maximum
     */
    static void requireCount(int value, int max, String field, String label) {
        if (value < 0 || value > max) {
            throw new InvalidDataException(field, label + " debe estar entre 0 y " + max + ".");
        }
    }

    private static String display(String number) {
        return number.replace('.', ',');
    }
}
