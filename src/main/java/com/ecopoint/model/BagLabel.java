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
     * Reads a label written as text, such as {@code AU-D3-M} or {@code AU-D3-M-2}.
     *
     * @param text label text
     * @return the label
     * @throws InvalidDataException if the text does not follow the label format
     */
    public static BagLabel parse(String text) {
        if (text == null || !text.matches("^[A-Z0-9]{2,6}-D[1-8]-[MT](-[0-9]+)?$")) {
            throw new InvalidDataException("rotulo", "El rótulo no tiene el formato FUENTE-D<día>-<M|T>.");
        }
        String[] parts = text.split("-");
        ShiftType shift = "M".equals(parts[2]) ? ShiftType.MANANA : ShiftType.TARDE;
        int sequence = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        return new BagLabel(parts[0], Integer.parseInt(parts[1].substring(1)), shift, sequence);
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
