package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

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

    /**
     * Validates every part of the label.
     *
     * @throws InvalidDataException if a part is missing or out of range
     */
    public BagLabel {
        if (!GenerationSource.isValidCode(sourceCode)) {
            throw new InvalidDataException("codigoFuente",
                    "El código de la fuente debe tener de 2 a 6 letras mayúsculas o dígitos.");
        }
        if (dayNumber < 1 || dayNumber > CharacterizationStudy.SAMPLING_DAYS) {
            throw new InvalidDataException("dia", "El día de muestreo debe estar entre 1 y " + CharacterizationStudy.SAMPLING_DAYS + ".");
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
