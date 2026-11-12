package com.ecopoint.model;

import com.ecopoint.exceptions.InvalidDataException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable entry of the correction history: one changed field of one record,
 * with its old and new value, the reason, who changed it and when (RN-063).
 * The original value is never lost.
 *
 * @param recordType kind of record, in Spanish (e.g. "Bolsa")
 * @param recordId   identifier of the corrected record
 * @param field      name of the changed field
 * @param oldValue   value before the correction
 * @param newValue   value after the correction
 * @param reason     reason written by the user
 * @param userId     identifier of the user who corrected the record
 * @param at         date and time of the correction
 * @author EstefaniaMarquez
 * @version 1.0
 */
public record Correction(String recordType, Long recordId, String field, String oldValue,
                         String newValue, String reason, Long userId, LocalDateTime at) {

    /**
     * Validates that the correction is complete and actually changes the value.
     *
     * @throws InvalidDataException if a required part is missing or the value does not change
     */
    public Correction {
        requireText(recordType, "tipoRegistro", "Debe indicarse el tipo de registro corregido.");
        requireText(field, "campo", "Debe indicarse el campo corregido.");
        requireText(reason, "motivo", "El motivo de la corrección es obligatorio.");
        if (userId == null) {
            throw new InvalidDataException("corregidoPor", "Debe registrarse quién hizo la corrección.");
        }
        if (at == null) {
            throw new InvalidDataException("fechaCorreccion", "Debe registrarse cuándo se hizo la corrección.");
        }
        if (Objects.equals(oldValue, newValue)) {
            throw new InvalidDataException(field, "El valor nuevo es igual al anterior: no hay nada que corregir.");
        }
        reason = reason.trim();
    }

    /**
     * Creates the correction of one field of a verifiable record.
     *
     * @param record   corrected record
     * @param field    name of the changed field
     * @param oldValue value before the correction
     * @param newValue value after the correction
     * @param reason   reason written by the user
     * @param userId   identifier of the user who corrected the record
     * @param at       date and time of the correction
     * @return the correction entry
     */
    public static Correction of(VerifiableRecord record, String field, Object oldValue, Object newValue,
                                String reason, Long userId, LocalDateTime at) {
        return new Correction(record.getRecordType(), record.getId(), field,
                Objects.toString(oldValue, null), Objects.toString(newValue, null), reason, userId, at);
    }

    private static void requireText(String value, String field, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(field, message);
        }
    }
}
