package com.ecopoint.service.report;

import com.ecopoint.service.StudyIndicators;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rounding of the reports, identical to the API (spec 006, R-511, R-533):
 * weights, percentages and density with 2 decimals, PPC with 3, HALF_UP.
 *
 * @author mlanderoc
 * @version 1.0
 */
public final class ReportNumbers {

    private static final int SCALE = 2;
    private static final int PPC_SCALE = 3;

    private ReportNumbers() {
    }

    /**
     * Rounds a weight, percentage or density.
     *
     * @param value value with full precision
     * @return the value with 2 decimals, or null
     */
    public static BigDecimal weight(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Rounds a per capita generation.
     *
     * @param value value with full precision
     * @return the value with 3 decimals, or null
     */
    public static BigDecimal ppc(BigDecimal value) {
        return value == null ? null : value.setScale(PPC_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Note on the shifts used in the composition (R-512, CA-505).
     *
     * @param composition composition of the study
     * @return the note, e.g. "Composición calculada con 12 de 14 turnos"
     */
    public static String compositionNote(StudyIndicators.Composition composition) {
        return "Composición calculada con " + composition.shiftsWithQuartering() + " de "
                + composition.expectedShifts() + " turnos";
    }
}
