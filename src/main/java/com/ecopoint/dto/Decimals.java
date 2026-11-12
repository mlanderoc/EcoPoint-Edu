package com.ecopoint.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding of the values the API shows: calculations keep their full precision
 * and are rounded only when they are returned (AGENTS.md, section 5).
 *
 * @author craulgomez
 * @version 1.0
 */
final class Decimals {

    private static final int SCALE = 2;

    private Decimals() {
    }

    /**
     * Rounds to 2 decimals with {@code HALF_UP}.
     *
     * @param value value to round, or null
     * @return the rounded value, or null
     */
    static BigDecimal show(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
