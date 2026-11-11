package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.util.regex.Pattern;

/**
 * Immutable label written on a waste bag, with the format
 * {@code SOURCE-D<day>-<M|T>} and an optional sequence when the same source
 * has more than one bag in the same shift, e.g. {@code AU-D3-M} or
 * {@code AU-D3-M-2} (RN-031).
 *
 * @param sourceCode generation source code (RN-002)
 * @param dayNumber  sampling day, 1 to 8
 * @param shiftType  morning or afternoon shift
 * @param sequence   bag number within the source and shift; 0 means no sequence
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record BagLabel(String sourceCode, int dayNumber, ShiftType shiftType, int sequence) {

    /** Valid source code: 2 to 6 uppercase letters or digits (RN-002). */
    public static final Pattern SOURCE_CODE_PATTERN = Pattern.compile("^[A-Z0-9]{2,6}$");

    /** Number of sampling days of a characterization study (RN-010). */
    public static final int STUDY_DAYS = 8;

    /**
     * Validates every part of the label.
     *
     * @throws InvalidDataException if a part is missing or out of range
     */
    public BagLabel {
        if (sourceCode == null || !SOURCE_CODE_PATTERN.matcher(sourceCode).matches()) {
            throw new InvalidDataException("codigoFuente",
                    "El código de la fuente debe tener de 2 a 6 letras mayúsculas o dígitos.");
        }
        if (dayNumber < 1 || dayNumber > STUDY_DAYS) {
            throw new InvalidDataException("dia", "El día de muestreo debe estar entre 1 y " + STUDY_DAYS + ".");
        }
        if (shiftType == null) {
            throw new InvalidDataException("turno", "El turno es obligatorio.");
        }
        if (sequence < 0) {
            throw new InvalidDataException("consecutivo", "El consecutivo de la bolsa no puede ser negativo.");
        }
    }

    /**
     * Creates the label of the only bag of a source in a shift (no sequence).
     *
     * @param sourceCode generation source code
     * @param dayNumber  sampling day, 1 to 8
     * @param shiftType  morning or afternoon shift
     * @return label without sequence, e.g. {@code AU-D3-M}
     */
    public static BagLabel of(String sourceCode, int dayNumber, ShiftType shiftType) {
        return new BagLabel(sourceCode, dayNumber, shiftType, 0);
    }

    /**
     * Returns a copy of this label with the given sequence number.
     *
     * @param newSequence bag number within the source and shift (1 or more)
     * @return label with sequence, e.g. {@code AU-D3-M-2}
     */
    public BagLabel withSequence(int newSequence) {
        return new BagLabel(sourceCode, dayNumber, shiftType, newSequence);
    }

    /**
     * Returns the label as it is written on the bag.
     *
     * @return text such as {@code AU-D3-M} or {@code AU-D3-M-2}
     */
    @Override
    public String toString() {
        String base = sourceCode + "-D" + dayNumber + "-" + shiftType.getCode();
        return sequence > 0 ? base + "-" + sequence : base;
    }
}
